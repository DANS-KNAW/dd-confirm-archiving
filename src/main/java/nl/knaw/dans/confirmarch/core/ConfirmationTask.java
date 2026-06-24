/*
 * Copyright (C) 2025 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.confirmarch.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.confirmarch.client.DataVaultClient;
import nl.knaw.dans.confirmarch.client.LobStoreClient;
import nl.knaw.dans.confirmarch.client.VaultCatalogClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // For testing
@Slf4j
public class ConfirmationTask implements Runnable {
    private final VaultCatalogClient vaultCatalogClient;
    private final Map<String, DataVaultClient> storageRootClients;
    private final Map<String, Path> storageRootProcessedDveDirs;
    private final Map<String, String> storageRootDatastations;
    private final LobStoreClient lobStoreClient;
    private final ObjectMapper objectMapper;
    private final int maxItemsPerRun;

    private int alternateOffset = 0;
    private boolean alternate = false;

    // Additional constructor for testing old tests
    ConfirmationTask(VaultCatalogClient vaultCatalogClient, Map<String, DataVaultClient> storageRootClients,
        Map<String, Path> storageRootProcessedDveDirs, int maxItemsPerRun) {
        this(vaultCatalogClient, storageRootClients, storageRootProcessedDveDirs, Map.of(), null, new ObjectMapper(), maxItemsPerRun, 0, false);
    }

    // Additional constructor for testing old tests
    ConfirmationTask(VaultCatalogClient vaultCatalogClient, Map<String, DataVaultClient> storageRootClients,
        Map<String, Path> storageRootProcessedDveDirs, int maxItemsPerRun, int alternateOffset, boolean alternate) {
        this(vaultCatalogClient, storageRootClients, storageRootProcessedDveDirs, Map.of(), null, new ObjectMapper(), maxItemsPerRun, alternateOffset, alternate);
    }

    @Override
    public void run() {
        log.debug("Starting confirmation task with maxItemsPerRun={}", maxItemsPerRun);
        try {
            // Get unconfirmed items from Vault Catalog
            int offset = alternate ? alternateOffset : 0;
            var unconfirmedItems = vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun, offset);
            log.debug("Found {} unconfirmed items at offset {} in {} mode", unconfirmedItems.size(), offset, alternate ? "alternate" : "normal");

            if (alternate && unconfirmedItems.isEmpty()) {
                log.debug("No unconfirmed items, resetting alternate offset to 0");
                alternateOffset = 0;
            }
            int numberConfirmed = 0;
            for (var item : unconfirmedItems) {
                var client = storageRootClients.get(item.getStorageRoot());
                if (client == null) {
                    log.error("No Data Vault client found for storage root '{}', skipping item datasetNbn={}, version={}",
                        item.getStorageRoot(), item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                    continue;
                }

                var creationTime = client.getCreationTime(item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                if (creationTime.isEmpty()) {
                    log.debug("Item datasetNbn={}, version={} not yet archived", item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                    continue;
                }

                var datastation = storageRootDatastations.get(item.getStorageRoot());
                if (datastation != null) {
                    if (!verifyLobs(client, datastation, item.getDatasetNbn(), item.getOcflObjectVersionNumber())) {
                        continue;
                    }
                }

                vaultCatalogClient.setArchivedTimestamp(item.getDatasetNbn(), item.getOcflObjectVersionNumber(), creationTime.get());
                log.info("Confirmed datasetNbn={}, version={}", item.getDatasetNbn(), item.getOcflObjectVersionNumber());

                // After confirmation, delete processed DVE zip files for this dataset/version in the storage root
                var processedDir = storageRootProcessedDveDirs.get(item.getStorageRoot());
                if (processedDir == null) {
                    log.warn("No processedDvesDir configured for storage root '{}', skipping cleanup for datasetNbn={}, version={}",
                        item.getStorageRoot(), item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                }
                else {
                    cleanupProcessedDveZips(processedDir, item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                }

                numberConfirmed++;
            }
            if (numberConfirmed > 0) {
                log.info("Confirmation task completed - {} items confirmed", numberConfirmed);
            }
            else {
                log.debug("Confirmation task completed - no items confirmed");
            }

            int numberFailedToConfirm = unconfirmedItems.size() - numberConfirmed;
            /*
             * In alternate mode we skip items that stayed unconfirmed. In normal mode we decrease the skip offset
             * for the alternate run by the number of confirmed items, because those items no longer need to be skipped.
             */
            if (alternate) {
                alternateOffset += numberFailedToConfirm;
            } else if (numberConfirmed > 0) {
                alternateOffset = 0;
            }
            alternate = !alternate;
        }
        catch (Exception e) {
            log.error("Error during confirmation task", e);
        }
    }

    private boolean verifyLobs(DataVaultClient client, String datastation, String nbn, int version) {
        try {
            var propertiesFile = client.getObjectExtensionFile(nbn, "object-version-properties/object_version_properties.json");
            if (propertiesFile.isEmpty()) {
                log.error("Properties file missing for datasetNbn={}, skipping confirmation", nbn);
                return false;
            }

            var properties = objectMapper.readValue(propertiesFile.get(), Map.class);
            var versionKey = "v" + version;
            @SuppressWarnings("unchecked")
            var versionProperties = (Map<String, Object>) properties.get(versionKey);

            if (versionProperties == null) {
                log.error("Properties for version {} missing for datasetNbn={}, skipping confirmation", versionKey, nbn);
                return false;
            }

            @SuppressWarnings("unchecked")
            var externalLargeObjects = (Map<String, Object>) versionProperties.get("external-large-objects");
            if (externalLargeObjects != null) {
                var checksumAlgorithm = (String) externalLargeObjects.get("checksum-algorithm");
                if (!"sha1".equalsIgnoreCase(checksumAlgorithm)) {
                    log.error("Unsupported checksum algorithm '{}' for datasetNbn={}, version={}, skipping confirmation", checksumAlgorithm, nbn, version);
                    return false;
                }

                @SuppressWarnings("unchecked")
                var lobs = (List<String>) externalLargeObjects.get("lobs");
                if (lobs != null) {
                    for (var hash : lobs) {
                        if (!lobStoreClient.isLobPresent(datastation, hash)) {
                            log.warn("LOB with hash {} not found in store {} for datasetNbn={}, version={}, skipping confirmation", hash, datastation, nbn, version);
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        catch (Exception e) {
            log.error("Error verifying LOBs for datasetNbn={}, version={}", nbn, version, e);
            return false;
        }
    }

    private void cleanupProcessedDveZips(Path processedDir, String nbn, int ocflVersion) {
        // Pattern: <nbn>_v<ocfl-version>[-<index-number>].zip
        var regex = Pattern.compile(Pattern.quote(nbn) + "_v" + ocflVersion + "(?:-\\d+)?\\.zip");
        if (!Files.isDirectory(processedDir)) {
            log.warn("processedDvesDir '{}' is not a directory for storage root; skipping cleanup for {} v{}", processedDir, nbn, ocflVersion);
            return;
        }
        try {
            var foundAny = false;
            try (var stream = Files.list(processedDir)) {
                for (var path : (Iterable<Path>) stream::iterator) {
                    var name = path.getFileName().toString();
                    if (regex.matcher(name).matches()) {
                        foundAny = true;
                        try {
                            Files.deleteIfExists(path);
                            log.debug("Deleted processed DVE file {}", path);
                        }
                        catch (IOException ioe) {
                            log.warn("Failed to delete processed DVE file {}: {}", path, ioe.getMessage());
                        }
                    }
                }
            }
            if (!foundAny) {
                log.warn("No processed DVE files found in '{}' for {} v{}", processedDir, nbn, ocflVersion);
            }
        }
        catch (IOException e) {
            log.warn("Failed to list processedDvesDir '{}' for cleanup: {}", processedDir, e.getMessage());
        }
    }
}

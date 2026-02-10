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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.confirmarch.client.DataVaultClient;
import nl.knaw.dans.confirmarch.client.VaultCatalogClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

@AllArgsConstructor
@Slf4j
public class ConfirmationTask implements Runnable {
    private final VaultCatalogClient vaultCatalogClient;
    private final Map<String, DataVaultClient> storageRootClients;
    private final Map<String, Path> storageRootProcessedDveDirs;
    private final int maxItemsPerRun;

    @Override
    public void run() {
        log.debug("Starting confirmation task with maxItemsPerRun={}", maxItemsPerRun);
        try {
            // Get unconfirmed items from Vault Catalog
            var unconfirmedItems = vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun);
            log.debug("Found {} unconfirmed items", unconfirmedItems.size());

            int numberConfirmed = 0;
            for (var item : unconfirmedItems) {
                var client = storageRootClients.get(item.getStorageRoot());
                if (client == null)        {
                    log.error("No Data Vault client found for storage root '{}', skipping item datasetNbn={}, version={}",
                        item.getStorageRoot(), item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                    continue;
                }

                var creationTime = client.getCreationTime(item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                if (creationTime.isEmpty()) {
                    log.debug("Item datasetNbn={}, version={} not yet archived", item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                    continue;
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
        }
        catch (Exception e) {
            log.error("Error during confirmation task", e);
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

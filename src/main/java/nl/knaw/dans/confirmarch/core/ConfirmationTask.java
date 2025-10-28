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

import java.util.Map;

@AllArgsConstructor
@Slf4j
public class ConfirmationTask implements Runnable {
    private final VaultCatalogClient vaultCatalogClient;
    private final Map<String, DataVaultClient> storageRootClients;
    private final int maxItemsPerRun;

    @Override
    public void run() {
        log.info("Starting confirmation task with maxItemsPerRun={}", maxItemsPerRun);
        try {
            // Get unconfirmed items from Vault Catalog
            var unconfirmedItems = vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun);
            log.debug("Found {} unconfirmed items", unconfirmedItems.size());

            int numberConfirmed = 0;
            for (var item : unconfirmedItems) {
                var client = storageRootClients.get(item.getStorageRoot());
                var creationTime = client.getCreationTime(item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                if (creationTime.isEmpty()) {
                    log.debug("Item datasetNbn={}, version={} not yet archived", item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                    continue;
                }
                vaultCatalogClient.setArchivedTimestamp(item.getDatasetNbn(), item.getOcflObjectVersionNumber(), creationTime.get());
                log.debug("Confirmed datasetNbn={}, version={}", item.getDatasetNbn(), item.getOcflObjectVersionNumber());
                numberConfirmed++;
            }
            log.info("Confirmation task completed - {} items confirmed", numberConfirmed);
        }
        catch (Exception e) {
            log.error("Error during confirmation task", e);
        }
    }
}

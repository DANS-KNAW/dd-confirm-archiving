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
import nl.knaw.dans.confirmarch.db.ConfirmationRequestDao;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@AllArgsConstructor
@Slf4j
public class ConfirmationTask implements Runnable {
    private final ConfirmationRequestDao confirmationRequestDao;
    private final String nbn;
    private final int version;
    private final DataVaultClient dataVaultClient;
    private final VaultCatalogClient vaultCatalogClient;

    @Override
    public void run() {
        var confirmationRequest = confirmationRequestDao.findByNbnAndVersion(nbn, version);
        if (confirmationRequest == null) {
            log.error("Confirmation request not found for nbn={}, version={}", nbn, version);
            return;
        }
        if (dataVaultClient.isArchived(nbn, version)) {
            log.error("Already archived: nbn={}, version={}", nbn, version);
        }
        else {
            log.info("Archiving: nbn={}, version={}", nbn, version);
            var archivalTimestamp = OffsetDateTime.now();
            vaultCatalogClient.setArchived(nbn, version, archivalTimestamp);
            var dve = confirmationRequest.getDvePath();

            log.info("Archiving completed for nbn={}, version={}", confirmationRequest.getNbn(), confirmationRequest.getVersion());
        }
    }

    private void deleteDve(Path dve) {
    }
}

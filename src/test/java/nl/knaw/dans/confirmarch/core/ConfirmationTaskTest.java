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

import nl.knaw.dans.confirmarch.client.DataVaultClient;
import nl.knaw.dans.confirmarch.client.VaultCatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class ConfirmationTaskTest {
    private ConfirmationTask confirmationTask;
    private VaultCatalogClient vaultCatalogClient;
    private DataVaultClient dataVaultClient1;
    private DataVaultClient dataVaultClient2;

    @BeforeEach
    public void setUp() {
        vaultCatalogClient = Mockito.mock(VaultCatalogClient.class);
        Map<String, DataVaultClient> storageRootClients = new HashMap<>();
        dataVaultClient1 = Mockito.mock(DataVaultClient.class);
        storageRootClients.put("root1", dataVaultClient1);
        dataVaultClient2 = Mockito.mock(DataVaultClient.class);
        storageRootClients.put("root2", dataVaultClient2);
        int maxItemsPerRun = 10;
        confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRootClients, maxItemsPerRun);
    }


    @Test
    public void should_do_nothing_when_no_unconfirmed_items() throws Exception {
        // Given
        Mockito.when(vaultCatalogClient.getUnconfirmedItems(Mockito.anyInt()))
               .thenReturn(java.util.Collections.emptyList());

        // When
        confirmationTask.run();
        // Then
        // No archived timestamps should be set
        Mockito.verify(vaultCatalogClient, Mockito.never()).setArchivedTimestamp(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
    }
}

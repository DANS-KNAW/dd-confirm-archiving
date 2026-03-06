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
import nl.knaw.dans.vaultcatalog.client.api.UnconfirmedDatasetVersionExportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfirmationTaskTest {
    private ConfirmationTask confirmationTask;
    private VaultCatalogClient vaultCatalogClient;
    private DataVaultClient dataVaultClient1;
    private DataVaultClient dataVaultClient2;
    private Map<String, DataVaultClient> storageRootClients;
    private Map<String, Path> processedDirs;
    private final int maxItemsPerRun = 10;

    @BeforeEach
    public void setUp() {
        vaultCatalogClient = mock(VaultCatalogClient.class);
        storageRootClients = new HashMap<>();
        dataVaultClient1 = mock(DataVaultClient.class);
        storageRootClients.put("root1", dataVaultClient1);
        dataVaultClient2 = mock(DataVaultClient.class);
        storageRootClients.put("root2", dataVaultClient2);
        processedDirs = new HashMap<>();
        processedDirs.put("root1", Path.of("/tmp/root1"));
        processedDirs.put("root2", Path.of("/tmp/root2"));
        confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRootClients, processedDirs, maxItemsPerRun);
    }


    @Test
    public void should_do_nothing_when_no_unconfirmed_items() throws Exception {
        // Given
        when(vaultCatalogClient.getUnconfirmedItems(anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        confirmationTask.run();
        // Then
        // No archived timestamps should be set
        verify(vaultCatalogClient, never()).setArchivedTimestamp(anyString(), anyInt(), any());
    }

    @Test
    public void normal_mode_should_reset_alternateOffset_to_zero_if_any_items_confirmed() throws Exception {
        // Given
        // Start in normal mode with offset 5
        confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRootClients, processedDirs, maxItemsPerRun, 5, false);

        var item1 = createItem("nbn1", 1, "root1");
        var item2 = createItem("nbn2", 1, "root1");
        // In normal mode offset is always 0
        when(vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun, 0))
            .thenReturn(List.of(item1, item2));

        // item1 is confirmed
        when(dataVaultClient1.getCreationTime("nbn1", 1)).thenReturn(Optional.of(OffsetDateTime.now()));
        // item2 is not yet archived
        when(dataVaultClient1.getCreationTime("nbn2", 1)).thenReturn(Optional.empty());

        // When
        confirmationTask.run();

        // Then
        verify(vaultCatalogClient).setArchivedTimestamp(eq("nbn1"), eq(1), any());
        verify(vaultCatalogClient, never()).setArchivedTimestamp(eq("nbn2"), eq(1), any());

        // alternateOffset was 5, 1 confirmed, so it should be reset to 0
        assertThat(getAlternateOffset(confirmationTask)).isEqualTo(0);
    }


    @Test
    public void alternate_mode_should_use_alternateOffset_and_increment_it() throws Exception {
        // Given
        // Start in alternate mode with offset 5
        confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRootClients, processedDirs, maxItemsPerRun, 5, true);

        var item1 = createItem("nbn1", 1, "root1");
        var item2 = createItem("nbn2", 1, "root1");
        // Should call getUnconfirmedItems with offset 5
        when(vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun, 5))
            .thenReturn(List.of(item1, item2));

        // Both items stay unconfirmed
        when(dataVaultClient1.getCreationTime(anyString(), anyInt())).thenReturn(Optional.empty());

        // When
        confirmationTask.run();

        // Then
        // alternateOffset should be 5 + 2 = 7
        assertThat(getAlternateOffset(confirmationTask)).isEqualTo(7);
    }

    @Test
    public void alternate_mode_should_increment_only_by_remaining_unconfirmed_items() throws Exception {
        // Given
        // Start in alternate mode with offset 5
        confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRootClients, processedDirs, maxItemsPerRun, 5, true);

        var item1 = createItem("nbn1", 1, "root1");
        var item2 = createItem("nbn2", 1, "root1");
        when(vaultCatalogClient.getUnconfirmedItems(maxItemsPerRun, 5))
            .thenReturn(List.of(item1, item2));

        // item1 is confirmed
        when(dataVaultClient1.getCreationTime("nbn1", 1)).thenReturn(Optional.of(OffsetDateTime.now()));
        // item2 is not yet archived
        when(dataVaultClient1.getCreationTime("nbn2", 1)).thenReturn(Optional.empty());

        // When
        confirmationTask.run();

        // Then
        // alternateOffset should be 5 + 1 = 6 (item1 confirmed, item2 remains)
        assertThat(getAlternateOffset(confirmationTask)).isEqualTo(6);
    }

    private UnconfirmedDatasetVersionExportDto createItem(String nbn, int version, String storageRoot) {
        var item = new UnconfirmedDatasetVersionExportDto();
        item.setDatasetNbn(nbn);
        item.setOcflObjectVersionNumber(version);
        item.setStorageRoot(storageRoot);
        return item;
    }

    private int getAlternateOffset(ConfirmationTask task) throws Exception {
        var field = ConfirmationTask.class.getDeclaredField("alternateOffset");
        field.setAccessible(true);
        return (int) field.get(task);
    }
}

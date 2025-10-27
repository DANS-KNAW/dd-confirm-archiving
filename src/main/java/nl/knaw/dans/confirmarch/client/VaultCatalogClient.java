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
package nl.knaw.dans.confirmarch.client;

import nl.knaw.dans.confirmarch.config.ServiceConfig;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.vaultcatalog.client.api.UnconfirmedDatasetVersionExportDto;
import nl.knaw.dans.vaultcatalog.client.invoker.ApiClient;
import nl.knaw.dans.vaultcatalog.client.invoker.ApiException;
import nl.knaw.dans.vaultcatalog.client.resources.DefaultApi;

import java.time.OffsetDateTime;
import java.util.List;

public class VaultCatalogClient {
    private final DefaultApi api;

    public VaultCatalogClient(ServiceConfig serviceConfig) {
        api = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .apiClient(new ApiClient())
            .defaultApiCtor(DefaultApi::new)
            .basePath(serviceConfig.getUrl())
            .httpClient(serviceConfig.getHttpClient())
            .build();
    }

    public List<UnconfirmedDatasetVersionExportDto> getUnconfirmedItems(int maxItemsPerRun) throws ApiException {
        return api.getUnconfirmedDatasetVersionExports(maxItemsPerRun, 0);
    }

    public void setArchivedTimestamp(String datasetNbn, Integer ocflObjectVersionNumber, OffsetDateTime creationTime) throws ApiException {
        api.setVersionExportArchivedTimestamp(datasetNbn, ocflObjectVersionNumber, creationTime);
    }
}

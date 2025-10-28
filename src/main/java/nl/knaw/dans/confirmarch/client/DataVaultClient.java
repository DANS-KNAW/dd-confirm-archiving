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

import io.dropwizard.client.JerseyClientConfiguration;
import nl.knaw.dans.confirmarch.config.ServiceConfig;
import nl.knaw.dans.datavault.client.invoker.ApiClient;
import nl.knaw.dans.datavault.client.invoker.ApiException;
import nl.knaw.dans.datavault.client.resources.DefaultApi;
import nl.knaw.dans.lib.util.ClientProxyBuilder;

import java.time.OffsetDateTime;
import java.util.Optional;

public class DataVaultClient {
    private final DefaultApi api;

    public DataVaultClient(ServiceConfig serviceConfig, JerseyClientConfiguration defaultHttpClient) {
        api = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .basePath(serviceConfig.getUrl())
            .apiClient(new ApiClient())
            .defaultApiCtor(DefaultApi::new)
            .httpClient(serviceConfig.getHttpClient() == null ? defaultHttpClient : serviceConfig.getHttpClient())
            .build();
    }

    public Optional<OffsetDateTime> getCreationTime(String nbn, int versionNumber) throws ApiException {
        try {
            var version = api.objectsIdVersionsNrGet(nbn, versionNumber);
            return Optional.ofNullable(version.getCreated());
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                return Optional.empty();
            }
            else {
                throw e;
            }
        }
    }

}

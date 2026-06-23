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
import lombok.Getter;
import nl.knaw.dans.confirmarch.config.ServiceConfig;
import nl.knaw.dans.datavault.client.invoker.ApiClient;
import nl.knaw.dans.datavault.client.invoker.ApiException;
import nl.knaw.dans.datavault.client.resources.DefaultApi;
import nl.knaw.dans.lib.util.ClientProxyBuilder;

import javax.ws.rs.core.GenericType;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;

@Getter
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

    public Optional<File> getObjectExtensionFile(String nbn, String path) throws ApiException {
        var apiClient = api.getApiClient();
        String localVarPath = "/ocfl/objects/{id}/extension-files/{path}"
            .replaceAll("\\{id}", apiClient.escapeString(nbn))
            .replaceAll("\\{path}", apiClient.escapeString(path));

        String localVarAccept = apiClient.selectHeaderAccept("application/octet-stream");
        String localVarContentType = apiClient.selectHeaderContentType();
        GenericType<File> localVarReturnType = new GenericType<File>() {};

        try {
            var response = apiClient.invokeAPI("OcflApi.ocflObjectsIdExtensionFilesPathGet", localVarPath, "GET", new ArrayList<>(), null,
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                null, localVarReturnType, false);
            return Optional.ofNullable(response.getData());
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }
}

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

package nl.knaw.dans.confirmarch;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import nl.knaw.dans.confirmarch.client.DataVaultClient;
import nl.knaw.dans.confirmarch.client.VaultCatalogClient;
import nl.knaw.dans.confirmarch.config.DdConfirmArchivingConfig;
import nl.knaw.dans.confirmarch.core.ConfirmationTask;
import nl.knaw.dans.lib.util.PingHealthCheck;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DdConfirmArchivingApplication extends Application<DdConfirmArchivingConfig> {
    public static void main(final String[] args) throws Exception {
        new DdConfirmArchivingApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Confirm Archiving";
    }

    @Override
    public void initialize(final Bootstrap<DdConfirmArchivingConfig> bootstrap) {

    }

    @Override
    public void run(final DdConfirmArchivingConfig configuration, final Environment environment) {
        var vaultCatalogClient = new VaultCatalogClient(configuration.getVaultCatalog(), configuration.getDefaultHttpClient());
        var storageRoots = new HashMap<String, DataVaultClient>();
        var processedDirs = new HashMap<String, Path>();
        for (var dataVaultConfig : configuration.getStorageRoots()) {
            storageRoots.put(dataVaultConfig.getOcflStorageRoot(), new DataVaultClient(dataVaultConfig, configuration.getDefaultHttpClient()));
            processedDirs.put(dataVaultConfig.getOcflStorageRoot(), Path.of(dataVaultConfig.getProcessedDvesDir()));
        }
        var confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRoots, processedDirs, configuration.getConfirmArchiving().getMaxItemsPerRun());

        // Register Vault Catalog ping health check
        environment.healthChecks().register(HealthChecks.VAULT_CATALOG, new PingHealthCheck(
            HealthChecks.VAULT_CATALOG,
            vaultCatalogClient.getApi().getApiClient().getHttpClient(),
            configuration.getVaultCatalog().getPingUrl()));

        for (var entry : storageRoots.entrySet()) {
            var storageRoot = entry.getKey();
            var client = entry.getValue();
            var dataVaultPingName = HealthChecks.DATA_VAULT + "@" + storageRoot;
            environment.healthChecks().register(dataVaultPingName, new PingHealthCheck(
                dataVaultPingName,
                client.getApi().getApiClient().getHttpClient(),
                // find matching config by storage root name
                configuration.getStorageRoots().stream()
                    .filter(c -> storageRoot.equals(c.getOcflStorageRoot()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing storage root config for " + storageRoot))
                    .getPingUrl()));
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(confirmationTask, 0, configuration.getConfirmArchiving().getRunEvery().toMilliseconds(), TimeUnit.MILLISECONDS);
    }

}

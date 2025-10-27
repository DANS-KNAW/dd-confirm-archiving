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
        for (var dataVaultConfig : configuration.getStorageRoots()) {
            storageRoots.put(dataVaultConfig.getOcflStorageRoot(), new DataVaultClient(dataVaultConfig, configuration.getDefaultHttpClient()));
        }
        var confirmationTask = new ConfirmationTask(vaultCatalogClient, storageRoots, configuration.getConfirmArchiving().getMaxItemsPerRun());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(confirmationTask, 0, configuration.getConfirmArchiving().getRunEvery().toMilliseconds(), TimeUnit.MILLISECONDS);
    }

}

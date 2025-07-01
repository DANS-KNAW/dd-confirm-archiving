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
import nl.knaw.dans.confirmarch.config.DdConfirmArchivingConfig;
import nl.knaw.dans.confirmarch.db.ConfirmationRequestDao;
import nl.knaw.dans.confirmarch.resources.ConfirmationRequestApiResource;

public class DdConfirmArchivingApplication extends Application<DdConfirmArchivingConfig> {
    private final DdConfirmArchivingHibernateBundle hibernateBundle = new DdConfirmArchivingHibernateBundle();

    public static void main(final String[] args) throws Exception {
        new DdConfirmArchivingApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Confirm Archiving";
    }

    @Override
    public void initialize(final Bootstrap<DdConfirmArchivingConfig> bootstrap) {
        bootstrap.addBundle(hibernateBundle);
    }

    @Override
    public void run(final DdConfirmArchivingConfig configuration, final Environment environment) {
        var confirmationRequestDao = new ConfirmationRequestDao(hibernateBundle.getSessionFactory());
        environment.jersey().register(new ConfirmationRequestApiResource(confirmationRequestDao));
    }

}

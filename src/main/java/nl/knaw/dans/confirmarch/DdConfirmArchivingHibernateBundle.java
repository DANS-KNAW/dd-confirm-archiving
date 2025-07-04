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

import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import nl.knaw.dans.confirmarch.config.DdConfirmArchivingConfig;
import nl.knaw.dans.confirmarch.core.ConfirmationRequest;

public class DdConfirmArchivingHibernateBundle extends HibernateBundle<DdConfirmArchivingConfig> {
    protected DdConfirmArchivingHibernateBundle() {
        super(ConfirmationRequest.class);
    }

    @Override
    public PooledDataSourceFactory getDataSourceFactory(DdConfirmArchivingConfig ddConfirmArchivingConfig) {
        return ddConfirmArchivingConfig.getDatabase();
    }
}

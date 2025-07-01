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
package nl.knaw.dans.confirmarch.db;

import io.dropwizard.hibernate.AbstractDAO;
import nl.knaw.dans.confirmarch.core.ConfirmationRequest;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

public class ConfirmationRequestDao extends AbstractDAO<ConfirmationRequest> {
    public ConfirmationRequestDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public ConfirmationRequest save(ConfirmationRequest confirmationRequest) {
        try {
            if (confirmationRequest.getId() == null || get(confirmationRequest.getId()) == null) {
                persist(confirmationRequest);
            }
            else {
                currentSession().update(confirmationRequest);
            }
            return confirmationRequest;
        }
        catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(e.getSQLException().getMessage());
        }
    }
}

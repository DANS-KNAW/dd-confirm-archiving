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
package nl.knaw.dans.confirmarch.resources;

import io.dropwizard.hibernate.UnitOfWork;
import lombok.AllArgsConstructor;
import nl.knaw.dans.confirmarch.Conversions;
import nl.knaw.dans.confirmarch.api.ConfirmationRequestDto;
import nl.knaw.dans.confirmarch.db.ConfirmationRequestDao;
import org.mapstruct.factory.Mappers;

import javax.ws.rs.core.Response;
import java.net.URI;

@AllArgsConstructor
public class ConfirmationRequestApiResource implements ConfirmationRequestsApi {
    private static final Conversions conversions = Mappers.getMapper(Conversions.class);

    private final ConfirmationRequestDao confirmationRequestDao;

    @Override
    @UnitOfWork
    public Response confirmationRequestsPost(ConfirmationRequestDto confirmationRequestDto) {
        // TODO: check for conflict.

        confirmationRequestDao.save(conversions.convert(confirmationRequestDto));
        return Response
            .created(URI.create("/confirmationRequests/" + confirmationRequestDto.getNbn() + "/versions/" + confirmationRequestDto.getVersion()))
            .build();
    }
}

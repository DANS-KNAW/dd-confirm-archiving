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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

@Entity
@Table(name = "confirmation_request",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = { "nbn", "version" })
       })
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nbn", nullable = false)
    private String nbn;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "dve_path", nullable = false)
    private String dvePath;

    @Column(name = "storage_root", nullable = false)
    private String storageRoot;

    @Column(name = "archival_timestamp", nullable = true)
    private OffsetDateTime archivalTimestamp;

    @Column(name = "dve_deleted", nullable = false)
    private Boolean dveDeleted;
}

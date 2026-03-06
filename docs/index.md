dd-confirm-archiving
=============

Registers the Data Vault archived status in the Vault Catalog

Purpose
-------
Polling the Vault Catalog for unconfirmed dataset versions, checking whether they are archived in the Data Vault, and registering the archived status in the
Vault Catalog.

Interfaces
----------

The service has the following interfaces:

![Interfaces](img/overview.png){width="70%"}

### Provided interfaces

#### Admin console

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: to monitor and manage the catalog service.

### Consumed interfaces

#### Vault Catalog API

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: to retrieve the list of unconfirmed dataset versions and to register the archived status of dataset versions.

#### Data Vault API

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: to get the archival timestamp of dataset versions.

Processing
----------

The service periodically polls the Vault Catalog for dataset versions that are not yet confirmed as archived. To avoid getting stuck on items that take longer
than expected to appear in the Data Vault, it alternates between two modes:

- **Normal mode** (every other run) starts from offset 0, checking the oldest unconfirmed items. If any items are successfully confirmed, the alternate mode is 
   reset to start at 0.
- **Alternate mode** (every other run) starts from the current alternate skip offset, moving past items that have previously failed to confirm. The skip offset
   is reset to 0 if no unconfirmed items are found. Otherwise, the skip offset is increased by the number of items that could not be confirmed in this run.

This strategy ensures that the service eventually reaches newer items even if a batch of older ones remains unconfirmed for a long time. Once the service is
restarted, the offset is reset to 0.

For each unconfirmed dataset version, it checks the Data Vault for its archival status. If it is archived, the service performs the following steps:

1. It retrieves the creation timestamp from the Data Vault and registers it in the Vault Catalog.
2. It deletes any processed DVE zip files for this dataset version from the configured `processedDvesDir` for the
   relevant storage root. The filenames are expected to follow the pattern `<nbn>_v<ocfl-version>[-<index-number>].zip`.

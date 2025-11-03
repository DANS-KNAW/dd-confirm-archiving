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

The service periodically polls the Vault Catalog for dataset versions that are not yet confirmed as archived. For each unconfirmed dataset version, it checks
the Data Vault to see if the dataset version is archived. If it is archived, the service registers the archived status in the Vault Catalog.

[![Build Status](https://travis-ci.org/EnMasseProject/storage-controller.svg?branch=master)](https://travis-ci.org/EnMasseProject/storage-controller)

# Storage Controller

The Storage Controller is responsible for managing brokers and its persistent volumes. The controller converts
destination addresses to replicated brokers for the different addresses and binds persistent volumes to
the brokers containers.

Generating the persistent volume claims and broker controllers can also be done manually using the storage-generator-tool.

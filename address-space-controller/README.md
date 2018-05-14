## Address Space Controller

The Address Space Controller is a global multitenant components that implements an API for creating and deleting EnMasse infrastructure instances as well as deploying and modifying the address configuration per instance. It combines the address configuration with the flavor configuration to find the appropriate OpenShift template to use for a given address, and instantiates the template with parameters as specified by the flavor.

# Build instructions

    make

[![Build Status](https://travis-ci.org/EnMasseProject/address-controller.svg?branch=master)](https://travis-ci.org/EnMasseProject/address-controller)

# Address Controller

The Address Controller is the API for deploying and modifying the address configuration in EnMasse. It combines the address configuration with the flavor configuration to find the appropriate OpenShift template to use for a given address, and instantiates the template with parameters as specified by the flavor.

The Address Controller expose an API for modifying the addressing config using either AMQP or HTTP.

# Build instructions

    gradle build


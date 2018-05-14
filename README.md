<a href="https://github.com/EnMasseProject/enmasse"><img src="https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/images/logo/enmasse_logo.png" width="50%" /></a>

[![Systemtest Status](https://travis-ci.org/EnMasseProject/enmasse.svg?branch=master)](https://travis-ci.org/EnMasseProject/enmasse)
[![GitHub release](https://img.shields.io/github/release/EnMasseProject/enmasse.svg)](https://github.com/EnMasseProject/enmasse/releases/latest)
[![Twitter Follow](https://img.shields.io/twitter/follow/enmasseio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/enmasseio)

--- 

EnMasse is an open source messaging platform, with focus on scalability and performance. EnMasse can run on your own infrastructure or in the cloud, and simplifies the deployment of messaging infrastructure. See our [web site](http://enmasse.io) for more information on what EnMasse is and what it can do.


# Internals

EnMasse is made of different components needed for the deployment and for handling the messaging infrastructure based on a Qpid Dispatch Router network and Apache ActiveMQ broker(s). Other broker implementations such as Apache Kafka may also be used.

* [address-space-controller](https://github.com/EnMasseProject/enmasse/tree/master/address-space-controller): Controls address spaces
* [standard-controller](https://github.com/EnMasseProject/enmasse/tree/master/standard-controller): Controls resources within the standard address space
* [api-server](https://github.com/EnMasseProject/enmasse/tree/master/api-server): REST API for creating address spaces and addresses
* [service-broker](https://github.com/EnMasseProject/enmasse/tree/master/service-broker): Open Service Broker API implementation for EnMasse
* [subserv](https://github.com/EnMasseProject/enmasse/tree/master/subserv): Subscription service for durable subscriptions
* [agent](https://github.com/EnMasseProject/enmasse/tree/master/agent): Messaging-centric console, managing and monitoring addresses on routers and brokers
* [mqtt-gateway](https://github.com/EnMasseProject/enmasse/tree/master/mqtt-gateway): MQTT gateway for "MQTT over AMQP"
* [mqtt-lwt](https://github.com/EnMasseProject/enmasse/tree/master/mqtt-lwt): MQTT Last Will and Testament Service for "MQTT over AMQP"
* [amqp-kafa-bridge](https://github.com/EnMasseProject/amqp-kafka-bridge): A bridge between AMQP and Apache Kafka

# Documentation

See the our [documentation](http://enmasse.io/documentation) for up to date documentation. We also have some [design documents](documentation/design_docs/design) for various parts of EnMasse.

# Getting help

See [contributing](http://enmasse.io/contributing) for more info on how to get help from the community.

If you encounter some issues during deployment, please check the following [page](documentation/design_docs/issues/issues.adoc) with
well known issues and related fixes/workaround.

# Developing

See [HACKING](HACKING.md) for details on how to build EnMasse. Submit patches using pull requests, or post patches to the mailing lists. See [contributing](http://enmasse.io/contributing) for more info.

# License

EnMasse is licensed under the [Apache License, Version 2.0](LICENSE)

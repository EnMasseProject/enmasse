<a href="https://github.com/EnMasseProject/enmasse"><img src="https://raw.githubusercontent.com/EnMasseProject/enmasse/master/documentation/logo/enmasse_logo.png" width="50%" /></a>

[![Systemtest Status](https://travis-ci.org/EnMasseProject/enmasse.svg?branch=master)](https://travis-ci.org/EnMasseProject/enmasse)
[![GitHub release](https://img.shields.io/github/release/EnMasseProject/enmasse.svg)](https://github.com/EnMasseProject/enmasse/releases/latest)

--- 

EnMasse is an open source messaging platform, with focus on scalability and performance. EnMasse can run on your own infrastructure or in the cloud, and simplifies the deployment of messaging infrastructure.

The EnMasse project that aims to create, as a community, an open source messaging platform that runs on [Kubernetes](https://kubernetes.io/) and [OpenShift](http://openshift.org/), using open standards like [AMQP](http://amqp.org/), [MQTT](http://mqtt.org/), HTTP etc. EnMasse is based on other open source projects like [Apache ActiveMQ Artemis](https://activemq.apache.org/artemis/), [Apache Qpid Dispatch Router](https://qpid.apache.org/components/dispatch-router/index.html) and finally the [Vert.x](http://vertx.io/) toolkit.

## Features

* Multiple <b>communication patterns</b>: request-response, pub-sub and events
* Support for <b>store and forward</b> and <b>direct messaging</b> mechanisms
* <b>Elastic scaling</b> of message brokers
* <b>AMQP</b> and <b>MQTT</b> support
* <b>Simple</b> setup, management and <b>monitoring</b>.
* <b>Multitenancy</b>: Manage multiple independent instances
* Built on <b>Kubernetes/OpenShift</b>: deploy <b>on-premise</b> or in the <b>cloud</b>

EnMasse can be used for many purposes, such as moving your messaging infrastructure to the cloud (without depending on a specific cloud provider) or building a scalable messaging backbone for IoT.

## Components

EnMasse is made of different components needed for the deployment and for handling the messaging infrastructure based on a Qpid Dispatch Router network and Apache ActiveMQ broker(s). Other broker implementations such as Apache Kafka may also be used.

* [address-controller](https://github.com/EnMasseProject/enmasse/tree/master/admin#address-controller): Controls multiple instances (tenants) and per-instance address space
* [ragent](https://github.com/EnMasseProject/enmasse/tree/master/ragent): Controls the router network configuration
* [configserv](https://github.com/EnMasseProject/enmasse/tree/master/admin#configserv): A bridge for subscribing to Kubernetes resource updates through AMQP
* [queue-scheduler](https://github.com/EnMasseProject/enmasse/tree/master/admin#queue-scheduler): Controls the mapping of queues to brokers
* [subserv](https://github.com/EnMasseProject/enmasse/tree/master/subserv): Subscription service for durable subscriptions
* [console](https://github.com/EnMasseProject/enmasse/tree/master/console): Messaging-centric console for managing and monitoring addresses
* [mqtt-gateway](https://github.com/EnMasseProject/enmasse/tree/master/mqtt/mqtt-gateway): MQTT gateway for "MQTT over AMQP"
* [mqtt-lwt](https://github.com/EnMasseProject/enmasse/tree/master/mqtt/mqtt-lwt): MQTT Last Will and Testament Service for "MQTT over AMQP"
* [amqp-kafa-bridge](https://github.com/EnMasseProject/amqp-kafka-bridge): A bridge between AMQP and Apache Kafka

# Getting started

See the [Getting Started](documentation/getting-started/README.adoc) for how to install EnMasse. For
more details on architecture and implementation, see [documentation](documentation/README.adoc).

# Getting help

Submit bugs and feature requests using the [issue tracker](https://github.com/EnMasseProject/enmasse/issues).

For questions or feedback, reach us on IRC on #enmasse on Freenode or post to our [mailing list](https://www.redhat.com/mailman/listinfo/enmasse).

If you encounter some issues during deployment, please check the following [page](documentation/issues/issues.md) with
well known issues and related fixes/workaround.

# Contributing

See [HACKING](HACKING.md) for details on how to build the different components of EnMasse. Submit patches using pull requests, or post patches to the mailing lists. See the [issues list](https://github.com/EnMasseProject/enmasse/issues) to get an idea of future plans and areas where you can contribute.

# License

EnMasse is licensed under the [Apache License, Version 2.0](LICENSE)

# EnMasse - Messaging as a Service
[![Systemtest Status](https://travis-ci.org/EnMasseProject/systemtests.svg?branch=master)](https://travis-ci.org/EnMasseProject/systemtests)

--- 
EnMasse is an elastic messaging platform running on OpenShift with focus on scalability and 
performance. EnMasse can run on your own infrastructure or in the cloud, and simplifies the 
deployment of messaging infrastructure.

EnMasse is based on open standards like [AMQP](http://amqp.org/) and [MQTT](http://mqtt.org/), and 
open source projects like [Apache ActiveMQ Artemis](https://activemq.apache.org/artemis/), [Apache Qpid Dispatch Router](https://qpid.apache.org/components/dispatch-router/index.html) and finally the [Vert.x](http://vertx.io/) toolkit for some of its components.

EnMasse is a great fit for IoT cloud infrastructure, and aims to extend protocol support to both
HTTP and CoAP.

## Features

* Multiple <b>communication patterns</b>: request-response, pub-sub and events
* Support for <b>store and forward</b> and <b>direct messaging</b> mechanisms
* <b>Elastic scaling</b> of message brokers
* <b>AMQP</b> and <b>MQTT</b> support
* <b>Simple</b> setup and management
* <b>Multitenancy</b>: Manage multiple independent instances
* Built on <b>OpenShift</b>: deploy <b>on-premise</b> or in the <b>cloud</b>

## Components

EnMasse is made of different components needed for the OpenShift deployment and for handling the messaging infrastructure based on a Qpid Dispatch Router network and Apache ActiveMQ broker(s).

* [address-controller](https://github.com/EnMasseProject/address-controller): Controls multiple instances (tenants) and per-instance address space
* [ragent](https://github.com/EnMasseProject/ragent): Controls the router network configuration
* [configserv](https://github.com/EnMasseProject/configserv): A bridge for subscribing to kubernetes resource updates through AMQP
* [queue-scheduler](https://github.com/EnMasseProject/queue-scheduler): Controls the mapping of queues to brokers
* [subserv](https://github.com/EnMasseProject/subserv): Subscription service for durable subscriptions
* [mqtt-gateway](https://github.com/EnMasseProject/mqtt-gateway): MQTT gateway for "MQTT over AMQP"
* [mqtt-lwt](https://github.com/EnMasseProject/mqtt-lwt): MQTT Last Will and Testament Service for "MQTT over AMQP"

# Getting started

See the [Getting Started](documentation/getting-started/README.md) for how to setup EnMasse. For
more details on architecture and implementation, see [documentation](documentation/README.md).

# Getting help

Submit bugs and feature requests using the [issue tracker](https://github.com/EnMasseProject/enmasse/issues).

For questions or feedback, reach us on IRC on #enmasse on Freenode or post to our [mailing list](https://www.redhat.com/mailman/listinfo/enmasse).

# Contributing

See [HACKING](HACKING.md) for details on how to build the different components of EnMasse. Submit patches using pull requests, or post patches to the mailing lists.

# License

EnMasse is licensed under the [Apache License, Version 2.0](LICENSE)

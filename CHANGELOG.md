## 0.13.0 (September 22, 2017)
* Added support for authentication. Users can now choose from 'none', 'standard' and 'external' as
  authentication services. See [authentication design doc](https://github.com/EnMasseProject/enmasse/blob/master/documentation/design/authentication.adoc)
  for details. The authentication services are used by both AMQP, MQTT and console endpoints.
* Secure *all* internal communication using TLS with server + client authentication. This ensures
  that no other component than those trusted by the EnMasse infrastructure can access services
  inside EnMasse.
* REST API for creating/deleting/listing address spaces, see [resource
  definitions](https://github.com/EnMasseProject/enmasse/blob/master/documentation/address-model/resource-definitions.adoc)
  for details.
* Use Apache ActiveMQ Artemis 2.2.0 as broker

35 [issues](https://github.com/EnMasseProject/enmasse/milestone/3?closed=1&page=1) has been resolved
for this release.

## 0.12.0 (August 4, 2017)
* Introduction of the [new address model](documentation/address-model/model.md), which changes how
  addresses are configured in EnMasse. This change comes from design work that has been done by
  the developer team in the past months, which made it apparent that we need to make the model more
  flexible in order to support other topologies than the pure router-based we have today.
* Integration with the OpenShift Service Catalog as an easy way to provision messaging on OpenShift.
* Improved status reporting for addresses. When querying the address controller for an Address,
  the status will now report ready: false if the routers are not yet configured with that address.
* Add support for external load balancers when running on Kubernetes (see templates/install/kubernetes/addons/external-lb.yaml)
* Use Apache Artemis 2.1.0 release as base for the broker
* Bugfixes to address-controller, configserv, console, queue-scheduler and router agent.

## 0.11.2 (June 2, 2017)
* Minor bugfixes to console and templates

## 0.11.0 (May 29, 2017)
* Remove restrictions on address names. These were earlier restructed by k8s labels
* Add simple logging library to router agent and subscription service
* Update Artemis broker flavor to 2.1.0
* New install 'bundle' containing both scripts and templates for deploying EnMasse
* Simple template for exposing Kubernetes services on cloud providers without ingress controllers

## 0.10.0 (May 3, 2017)
* Merged non-TLS and TLS-based templates into one, making EnMasse TLS-enabled by default. This
  simplifies deployment of EnMasse but also the maintenance of the templates.
* Guide for deploying EnMasse on AWS

## 0.9.0 (April 27, 2017)
* Various UI fixes to the console for demo purposes
* Improved documentation for deploying EnMasse with Open Service Broker API
* Dashboard for router metrics

## 0.8.0 (April 21, 2017)
* Support for pushing router metrics to [Hawkular](http://www.hawkular.org/) using the [Hawkular OpenShift Agent](https://github.com/hawkular/hawkular-openshift-agent)

## 0.7.0 (April 18, 2017)
* Support for pushing broker metrics to [Hawkular](http://www.hawkular.org/) using the [Hawkular OpenShift Agent](https://github.com/hawkular/hawkular-openshift-agent)
* A messaging console providing an overview of addresses and per-address metrics
* Support for deploying EnMasse on Kubernetes

## 0.5.0 (March 23, 2017)

* MQTT Last Will and Testament Service
* Basic support for multiple isolated address spaces with their own routers and brokers
* Support for the [Open Service Broker API](https://www.openservicebrokerapi.org/)

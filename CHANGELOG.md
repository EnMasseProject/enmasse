## 0.11.0 (May 29, 2027)
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

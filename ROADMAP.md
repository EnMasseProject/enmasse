# EnMasse Roadmap

This roadmap describes the features we expect to be available in the next releases of EnMasse. The list of features for a particular version is subject to change, particularily for versions further down the road.

0.17.0
* Support for plans to set resource limits for address spaces
* Rework queue-scheduling to take resource limits into account, automatically scale colocated
  broker if allowed, and take previous placement into consideration
* Use statefulsets for brokers

0.18.0
* Add support for colocated topics together with queues
* Support using wildcard certificates for external routes
* Change service broker to operate on address spaces and support binding
* Support viewing status of addresses in console

0.19.0
* Make mqtt components deployed on-demand instead
* OAUTH support in keycloak plugin and console
* ...

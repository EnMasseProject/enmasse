# EnMasse Roadmap

This roadmap describes the features we expect to be available in the next releases of EnMasse. The list of features for a particular version is subject to change, particularily for versions further down the road.

0.17.0
* Support for plans to set resource limits for address spaces
* Rework queue-scheduling to take resource limits into account, automatically scale colocated
  broker if allowed, and take previous placement into consideration
* Add support for colocated topics together with queues
* Change address model to require address field and make name field optional

0.18.0
* Support using wildcard certificates for external routes
* Change service broker to operate on address spaces and support binding
* Support viewing status of addresses in console
* Allow deploying without mqtt components

0.19.0
* Make mqtt components deployed on-demand
* OAUTH support in keycloak plugin and console
* ...

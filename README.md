# routilities

Various utilities for managing and monitoring a router network.

## console-server [WIP]

A patternfly based GUI for monitoring EnMasse instances. The console
can be accessed on port 8080.

For development purposes, it can be run outside of the local openshift
cluster, connecting to the messaging service and the config and
podsense daemons within the admin service by setting the
MESSAGING_SERVICE_HOST and ADMIN_SERVICE_HOST appropriately (e.g. with
$(oc get service | awk '/messaging/{print $2}') and $(oc get service |
awk '/admin/{print $2}') respectively).

## coming soon...

ragent and subserv will be moved into this repo for simpler sharing of
common code

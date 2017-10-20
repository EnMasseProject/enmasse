# EnMasse agent

Generic EnMasse agent running within each address space.

## brokerd address space controller

Controller for managing addresses in a brokered address space.

## console-server [WIP]

A patternfly based GUI for monitoring EnMasse instances. The console
can be accessed on port 8080.

For development purposes, it can be run outside of the local openshift
cluster, connecting to the messaging service and the config and
podsense daemons within the admin service by setting the
MESSAGING_SERVICE_HOST, ADDRESS_CONTROLLER_SERVICE_HOST and
ADMIN_SERVICE_HOST appropriately (e.g. with $(oc get service | awk
'/messaging/{print $2}'), $(oc get service | awk
'/address-controller/{print $2}') and $(oc get service | awk
'/admin/{print $2}') respectively).

## coming soon...

ragent and subserv will be moved into this repo for simpler sharing of
common code. The standard address space controller logic will also be moved here.

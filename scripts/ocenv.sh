#!/bin/sh
HOST=${1:-localhost}
export OPENSHIFT_USER=developer
export OPENSHIFT_TOKEN=`oc config view -o jsonpath="{.users[?(@.name == \"developer/${HOST}:8443\")].user.token}"`
export OPENSHIFT_URL=https://${HOST}:8443
export OPENSHIFT_NAMESPACE=myproject

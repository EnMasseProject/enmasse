#!/bin/sh
export OPENSHIFT_USER=developer
export OPENSHIFT_TOKEN=`oc config view -o jsonpath='{.users[?(@.name == "developer/localhost:8443")].user.token}'`
export OPENSHIFT_URL=https://localhost:8443
export OPENSHIFT_NAMESPACE=myproject

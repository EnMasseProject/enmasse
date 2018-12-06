#!/bin/sh
HOST=${1:-localhost}
NAMESPACE=${2:-myproject}
USER=${3:-developer}
REGISTER_API_SERVER=${4:-true}

export OPENSHIFT_USER=${USER}
export OPENSHIFT_USE_TLS=true
export OPENSHIFT_TOKEN=$(oc whoami -t)
export OPENSHIFT_URL=https://${HOST}:8443
export OPENSHIFT_PROJECT=${NAMESPACE}
export REGISTER_API_SERVER=${REGISTER_API_SERVER}

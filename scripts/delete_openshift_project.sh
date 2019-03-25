#!/usr/bin/env bash

# Deletes Openshift project stuck in the finalize state
# Usage: delete_openshift_project enmasse-infra ec2-x-x-x-x.zone.compute.amazonaws.com:8443

TOKEN=$(oc whoami -t)
# This doesn't work with ec2-x-x-x-x.zone.compute.amazonaws.com format of host names
#ENDPOINT=$(oc config current-context | cut -d/ -f2 | tr - .)
ENDPOINT="$2"
PROJECT="$1"

kubectl get namespace "$PROJECT" -o json | jq 'del(.spec.finalizers[] | select("kubernetes"))' | curl --insecure -k -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -X PUT --data-binary @- https://$ENDPOINT/api/v1/namespaces/$PROJECT/finalize
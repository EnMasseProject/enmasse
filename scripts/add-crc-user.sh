#!/bin/bash
# Adds (or updates) user to a CRC cluster's httpd passwd database
# Based on https://github.com/code-ready/crc/wiki/Add-another-user-to-cluster
#
# Usage:
#
# add-crc-user <user>

USER=$1
set -e

HTPASSWD_FILE=$(mktemp)

function finish() {
  rm ${HTPASSWD_FILE}
}

trap finish EXIT

oc get secret htpass-secret -n openshift-config --output=go-template='{{.data.htpasswd}}' | base64 --decode > ${HTPASSWD_FILE}

htpasswd ${HTPASSWD_FILE} ${USER}

oc create secret generic htpass-secret --from-file=htpasswd=${HTPASSWD_FILE} -n openshift-config --dry-run -o yaml | oc replace -f -



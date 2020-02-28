#!/usr/bin/env bash
#
# Copyright 2020, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

# Verifies that the console-server is minimally functional

KUBE_TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)

curl 'http://localhost:9090/graphql/query' \
-H 'Accept-Encoding: gzip, deflate, br' \
-H 'Content-Type: application/json' \
-H 'Accept: application/json' \
-H 'Origin: http://localhost:9090' \
-H "X-Forwarded-Access-Token: ${KUBE_TOKEN}" \
-H "X-Health: true" \
--data-binary '{"operationName": "health_probe_whoami", "query":"query health_probe_whoami {\n  whoami {\n    metadata {\n      name\n    }\n  }\n}"}' \
--compressed \
--fail
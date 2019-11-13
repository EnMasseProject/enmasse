#!/bin/bash
set -e
set -x
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

DOCKER_ORG=${DOCKER_ORG:-$USER}
TAG=${TAG:-latest}
if use_external_registry
then
    export IMAGE_VERSION=${TAG}
else
    export DOCKER_REGISTRY="localhost:5000"
fi

cat<<EOF | kubectl create -f -
apiVersion: operators.coreos.com/v1
kind: CatalogSource
metadata:
  name: enmasse-ocs
  namespace: 
  labels:
    operator: test
spec:
  displayName: EnMasse Community Operator
  image: ${DOCKER_REGISTRY}/${DOCKER_ORG}/olm-manifest:${TAG}
  publisher: travis
  sourceType: grpc
EOF

cat<<EOF | kubectl create -f -
apiVersion: operators.coreos.com/v1
kind: Subscription
metadata:
  name: enmasse-sub
  metadata: operators
spec:
  name: enmasse
  source: enmasse-ocs
  sourceNamespace: operators
  startingCSV: enmasse.0.30-SNAPSHOT
  channel: alpha
EOF

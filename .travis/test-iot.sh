#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

TAG=${TAG:-latest}

DEPLOY_IOT=true time ./systemtests/scripts/run_test_kubernetes.sh templates/build/enmasse-${TAG} "smoke-iot"

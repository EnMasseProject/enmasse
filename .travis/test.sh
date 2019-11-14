#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running smoke tests"
time make SYSTEMTEST_PROFILE=smoke systemtests

echo "Running OLM tests"
time make SYSTEMTEST_ARGS=OperatorLifecycleManagerTest systemtests

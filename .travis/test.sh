#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

VERSION=$(grep "release.version" pom.properties| cut -d'=' -f2)
TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running smoke tests"
time make SYSTEMTEST_PROFILE=smoke systemtests

echo "Running OLM tests"
time make SYSTEMTEST_ARGS=OperatorLifecycleManagerTest systemtests

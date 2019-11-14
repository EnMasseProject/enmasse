#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

TAG=${TAG:-latest}
SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS:-"io.enmasse.**.SmokeTest"}
SYSTEMTEST_PROFILE=${SYSTEMTEST_PROFILE:-"smoke"}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running systemtests ${SYSTEMTEST_ARGS} using profile ${SYSTEMTEST_PROFILE}"
time make SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS} SYSTEMTEST_PROFILE=${SYSTEMTEST_PROFILE} systemtests

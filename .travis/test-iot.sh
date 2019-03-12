#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

TAG=${TAG:-latest}
SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS:-"io.enmasse.**.SmokeTest"}
SYSTEMTEST_PROFILE=${SYSTEMTEST_PROFILE:-"smoke"}

time ./systemtests/scripts/run_test_kubernetes.sh templates/build/enmasse-${TAG} ${SYSTEMTEST_PROFILE} ${SYSTEMTEST_ARGS}

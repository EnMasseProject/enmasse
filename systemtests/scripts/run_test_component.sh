#!/bin/bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

ENMASSE_DIR=$1
KUBEADM=$2
TESTCASE=${3:-"io.enmasse.**"}
TEST_PROFILE=${4}
failure=0

SANITIZED_PROJECT=$OPENSHIFT_PROJECT
SANITIZED_PROJECT=${SANITIZED_PROJECT//_/-}
SANITIZED_PROJECT=${SANITIZED_PROJECT//\//-}
export OPENSHIFT_PROJECT=$SANITIZED_PROJECT

setup_test ${ENMASSE_DIR} ${KUBEADM}
if [ $? -ne 0 ]; then
    echo "DEPLOYMENT FAILED - tests won't be executed."
    exit 1
fi

#environment info before tests
LOG_DIR="${ARTIFACTS_DIR}/openshift-info/"
mkdir -p ${LOG_DIR}
get_kubernetes_info ${LOG_DIR} services default "-before"
get_kubernetes_info ${LOG_DIR} pods default "-before"

${CURDIR}/system-stats.sh > ${ARTIFACTS_DIR}/system-resources.log &
STATS_PID=$!

${CURDIR}/docker-logs.sh ${ARTIFACTS_DIR}/docker-logs > /dev/null 2> /dev/null &
LOGS_PID=$!

echo "process for checking system resources is running with PID: ${STATS_PID}"

if [ "${TEST_PROFILE}" = "systemtests-marathon" ]; then
    run_test ${TESTCASE} ${TEST_PROFILE} || failure=$(($failure + 1))
else
    run_test ${TESTCASE} systemtests-shared || failure=$(($failure + 1))
    run_test ${TESTCASE} systemtests-isolated || failure=$(($failure + 1))
fi


echo "process for checking system resources with PID: ${STATS_PID} will be killed"
kill ${STATS_PID}

echo "process for syncing docker logs with PID: ${LOGS_PID} will be killed"
kill ${LOGS_PID}

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test ${OPENSHIFT_PROJECT}
fi

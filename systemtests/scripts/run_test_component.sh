#!/bin/bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

ENMASSE_DIR=$1
KUBEADM=$2
SYSTEMTESTS=$3
TESTCASE=$4
failure=0

SANITIZED_PROJECT=$OPENSHIFT_PROJECT
SANITIZED_PROJECT=${SANITIZED_PROJECT//_/-}
SANITIZED_PROJECT=${SANITIZED_PROJECT//\//-}
export OPENSHIFT_PROJECT=$SANITIZED_PROJECT

setup_test ${ENMASSE_DIR} ${KUBEADM}

#environment info before tests
LOG_DIR="${ARTIFACTS_DIR}/openshift-info/"
mkdir -p ${LOG_DIR}
get_kubernetes_info ${LOG_DIR} services default "-before"
get_kubernetes_info ${LOG_DIR} pods default "-before"

${CURDIR}/system-stats.sh > ${ARTIFACTS_DIR}/system-resources.log &
STATS_PID=$!
echo "process for checking system resources is running with PID: ${STATS_PID}"

run_test ${TESTCASE} || failure=$(($failure + 1))

echo "process for checking system resources with PID: ${STATS_PID} will be killed"
kill ${STATS_PID}

#environment info after tests
${CURDIR}/store_kubernetes_info.sh ${LOG_DIR} ${OPENSHIFT_PROJECT}

#store artifacts
${CURDIR}/collect_logs.sh ${ARTIFACTS_DIR}

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    oc get events
    exit 1
else
    teardown_test ${OPENSHIFT_PROJECT}
fi

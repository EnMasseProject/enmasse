#!/usr/bin/env bash
CURDIR=$(readlink -f $(dirname $0))
source ${CURDIR}/test_func.sh

LOG_DIR=${1}
OPENSHIFT_PROJECT=${2}

#environment info
get_previous_logs ${LOG_DIR} ${OPENSHIFT_PROJECT}
get_previous_logs ${LOG_DIR} "default"

get_kubernetes_info ${LOG_DIR} pv ${OPENSHIFT_PROJECT}
get_kubernetes_info ${LOG_DIR} pods ${OPENSHIFT_PROJECT}
get_kubernetes_info ${LOG_DIR} services default "-after"
get_kubernetes_info ${LOG_DIR} pods default "-after"

get_docker_info ${LOG_DIR} origin



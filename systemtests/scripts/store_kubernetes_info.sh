#!/usr/bin/env bash
CURDIR=$(readlink -f $(dirname $0))
source ${CURDIR}/test_func.sh

LOG_DIR=${1}

#environment info
for namespace in `kubectl get namespaces -o jsonpath={.items[*].metadata.name}`
do
    NS_LOG_DIR=$LOG_DIR/$namespace
    mkdir -p $NS_LOG_DIR
    get_previous_logs ${NS_LOG_DIR} $namespace

    get_kubernetes_info ${NS_LOG_DIR} pv ${namespace} "-after"
    get_kubernetes_info ${NS_LOG_DIR} pods ${namespace} "-after"
    get_kubernetes_info ${NS_LOG_DIR} services ${namespace} "-after"
done

get_all_events ${LOG_DIR}
get_docker_info ${LOG_DIR} origin

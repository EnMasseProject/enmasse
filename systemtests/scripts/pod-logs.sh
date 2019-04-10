#!/usr/bin/env bash
LOG_DIR=$1
NAMESPACE=$2

mkdir -p ${LOG_DIR}

if which oc &> /dev/null; then
    CMD=oc
elif which kubectl &> /dev/null; then
    CMD=kubectl
else
    err_and_exit "Cannot find oc or kubectl command, please check path to ensure it is installed"
fi

while [[ true ]]; do
    PODS=$(${CMD} get pods -n ${NAMESPACE} -o go-template --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')
    for POD in ${PODS}; do
        CONTAINERS=$(${CMD} get pods ${POD} -o=jsonpath='{.spec.containers[*].name}')
        for CONTAINER in ${CONTAINERS}; do
            if [[ $(${CMD} logs ${POD} --container=${CONTAINER}) ]]; then
                ${CMD} logs ${POD} --container=${CONTAINER} > ${LOG_DIR}/${POD}.${CONTAINER}.log
            fi
        done
    done
    sleep 2
done

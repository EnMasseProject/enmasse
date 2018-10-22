#!/usr/bin/env bash
EXPECTED_PODS=$1
NAMESPACE=$2
UPGRADED=${3:-false}
CURDIR="$(readlink -f $(dirname $0))"
source ${CURDIR}/test_func.sh
source "${CURDIR}/../../scripts/logger.sh"


if which oc &> /dev/null; then
    CMD=oc
elif which kubectl &> /dev/null; then
    CMD=kubectl
else
    err_and_exit "Cannot find oc or kubectl command, please check path to ensure it is installed"
fi

if [[ "${UPGRADED}" == "true" ]]; then
    sleep 300
    EXPECTED_PODS=$(($($CMD get pods -n ${NAMESPACE} | grep -v deploy | wc -l) - 1))
fi
info "Expected pods: ${EXPECTED_PODS}"

function waitingContainersReady {
    ADDR_SPACE=$1
    pods_id=$($CMD get pods -n ${NAMESPACE} | awk 'NR >1 {print $1}')
    for pod_id in ${pods_id}
    do
        ready=$($CMD get -o json pod -n ${ADDR_SPACE}  ${pod_id} -o jsonpath={.status.containerStatuses[0].ready})
        if [[ "${UPGRADED}" == "true" ]]; then
            image=$($CMD get pod ${pod_id} -o jsonpath={.spec.containers[*].image})
            upgraded=$(is_upgraded ${image})
            if [[ "${upgraded}" == "true" ]]; then
                info "Pod ${pod_id} is upgraded to ${image}"
            else
                info "Pod ${pod_id} is not upgraded, current image: ${image}"
            fi
        else
            upgraded="true"
        fi
        if [[ "${ready}" == "false" ]] || [[ "${upgraded}" == "false" ]]
        then
            return 1
        fi
    done
    info "All containers are ready"
    return 0
}

TIMEOUT=600
NOW=$(date +%s)
END=$(($NOW + $TIMEOUT))
info "Waiting until ${END}"
while true
do
    NOW=$(date +%s)
    if [ $NOW -gt $END ]; then
        err "Timed out waiting for nodes to come up!"
        pods=`$CMD get pods -n ${NAMESPACE}`
        err_and_exit "PODS: ${pods}"
    fi
    num_running=`$CMD get pods -n ${NAMESPACE}| grep -v deploy | grep -c Running`
    if [ "$num_running" -eq "$EXPECTED_PODS" ]; then
        waitingContainersReady ${NAMESPACE}
        if [ $? -gt 0 ]
        then
            info "All pods are up but all containers are not ready yet"
        else
            info "ALL UP!"
            exit 0
        fi
    else
        info "$num_running/$EXPECTED_PODS up"
    fi
    sleep 5
done

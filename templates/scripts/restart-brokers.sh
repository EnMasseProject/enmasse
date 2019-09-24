#!/usr/bin/env bash
# Usage: ./restart-brokers.sh <namespace where EnMasse is running> <minimum number of pods that should run at any given time>
ENMASSE_NAMESPACE=${1:-enmasse-infra}
MINREADY=${2:-0}
MINAVAILABLE=$(($MINREADY + 1))

function wait_ready() {
    local kind=${1}
    local rset=${2}
    local minReady=${3}

    ready="0"
    while [[ "${ready}" -lt "${minReady}" ]]
    do
        ready=$(kubectl get ${kind} ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE})
        if [[ "${ready}" -lt "${minReady}" ]]; then
            echo "Minimum ${ready}/${minReady} pods ready"
            sleep 5
        fi
    done
    echo "Minimum ready pods ${minReady} restored"
}



for rset in $(kubectl get statefulset -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    if [[ "${rset}" == broker-* ]]; then
        wait_ready statefulset ${rset} $MINAVAILABLE
        infraUuid=$(kubectl get statefulset ${rset} -o jsonpath='{.metadata.labels.infraUuid}')

        echo "All broker pods in broker set ${rset} are ready. Initiating rolling restart."
        for rpod in $(kubectl get pods -l role=broker,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}')
        do
            if [[ "${rpod}" == ${rset}* ]]; then
                echo "Deleting broker pod ${rpod}"
                kubectl delete pod ${rpod} -n ${ENMASSE_NAMESPACE}
                sleep 30
                wait_ready statefulset ${rset} ${MINAVAILABLE}
                echo "Minimum ready broker replicas ${MINAVAILABLE} restored"
            fi
        done
    fi
done

for rset in $(kubectl get deployment -l role=broker -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    if [[ "${rset}" == broker* ]]; then
        wait_ready deployment ${rset} $MINAVAILABLE
        infraUuid=$(kubectl get deployment ${rset} -o jsonpath='{.metadata.labels.infraUuid}')

        echo "All broker pods in broker set ${rset} are ready. Initiating rolling restart."
        for rpod in $(kubectl get pods -l role=broker,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}')
        do
            if [[ "${rpod}" == ${rset}* ]]; then
                echo "Deleting broker pod ${rpod}"
                kubectl delete pod ${rpod} -n ${ENMASSE_NAMESPACE}
                sleep 30
                wait_ready deployment ${rset} $MINAVAILABLE
            fi
        done
    fi
done
echo "Broker restart complete!"

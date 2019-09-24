#!/usr/bin/env bash
ENMASSE_NAMESPACE=${1:-enmasse-infra}
MINREADY=${2:-1}
MINAVAILABLE=$(($MINREADY + 1))

function wait_statefulset_ready() {
    local rset=${1}
    local minReady=${2}

    ready=0
    while [[ "${ready}" -lt "${minReady}" ]]
    do
        ready=$(kubectl get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE})
        if [[ "${ready}" -lt "${minReady}" ]]; then
            echo "Minimum ${ready}/${minReady} pods ready"
            sleep 5
        fi
    done
    echo "Minimum ready pods ${minReady} restored"
}

for rset in $(kubectl get statefulset -l name=qdrouterd -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    wait_statefulset_ready ${rset} ${MINAVAILABLE}

    infraUuid=$(kubectl get statefulset ${rset} -o jsonpath='{.metadata.labels.infraUuid}')
    
    echo "All pods in router set ${rset} are ready. Initiating rolling restart."
    for rpod in $(kubectl get pods -l capability=router,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}')
    do
        echo "Deleting router pod ${rpod}"
        kubectl delete pod ${rpod} -n ${ENMASSE_NAMESPACE}
        sleep 30
        wait_statefulset_ready ${rset} ${MINAVAILABLE}
        echo "Minimum ready router replicas ${MINAVAILABLE} restored"
    done
done
echo "Router restart complete!"

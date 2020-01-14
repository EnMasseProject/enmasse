#!/usr/bin/env bash
# Usage: ./restart-operators.sh <namespace where EnMasse is running> <minimum number of pods that should run at any given time>
ENMASSE_NAMESPACE=${1:-enmasse-infra}
MINREADY=${2:-0}
MINAVAILABLE=$(($MINREADY + 1))

function wait_deployment_ready() {
    local dep=${1}
    local minReady=${2}

    ready=0
    while [[ "${ready}" -lt "${minReady}" ]]
    do
        ready=$(kubectl get deployment ${dep} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE})
        if [[ "${ready}" -lt "${minReady}" ]]; then
            echo "Minimum ${ready}/${minReady} pods ready"
            sleep 5
        fi
    done
    echo "Minimum ready pods ${minReady} restored"
}

echo "Restarting Address Space Controller"
kubectl delete pod -l name=address-space-controller
for dep in $(kubectl get deployment -l name=address-space-controller -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    wait_deployment_ready ${dep} 1
done
echo "Address Space Controller restarted"


echo "Restarting Operator"
kubectl delete pod -l name=enmasse-operator -n ${ENMASSE_NAMESPACE}
for dep in $(kubectl get deployment -l name=enmasse-operator -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    wait_deployment_ready ${dep} 1
done
echo "Operator restarted"

echo "Restarting standard operators"
for dep in $(kubectl get deployment -l name=admin -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    wait_deployment_ready ${dep} ${MINAVAILABLE}
    infraUuid=$(kubectl get deployment ${dep} -o jsonpath='{.metadata.labels.infraUuid}')

    echo "All admin pods are ready. Initiating rolling restart."
    for pod in $(kubectl get pods -l name=admin,infraUuid=$infraUuid -o jsonpath='{.items[*].metadata.name}')
    do
        echo "Deleting admin ${pod}"
        kubectl delete pod ${pod} -n ${ENMASSE_NAMESPACE}
        sleep 30
        wait_deployment_ready ${dep} ${MINAVAILABLE}
    done
done
echo "Standard operators restarted"

echo "Restarting brokered operators"
for dep in $(kubectl get deployment -l role=agent -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    wait_deployment_ready ${dep} ${MINAVAILABLE}
    infraUuid=$(kubectl get deployment ${dep} -o jsonpath='{.metadata.labels.infraUuid}')

    echo "All agent pods are ready. Initiating rolling restart."
    for pod in $(kubectl get pods -l role=agent,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}')
    do
        echo "Deleting agent ${pod}"
        kubectl delete pod ${pod} -n ${ENMASSE_NAMESPACE}
        sleep 30
        wait_deployment_ready ${dep} ${MINAVAILABLE}
    done
done
echo "Brokered operators restarted"

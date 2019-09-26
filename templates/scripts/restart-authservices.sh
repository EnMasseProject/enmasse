#!/usr/bin/env bash
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

echo "Restarting Authentication Services"
for dep in $(kubectl get authenticationservices -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
do
    deployment_name=$(kubectl get authenticationservice ${dep} -o jsonpath='{.spec.standard.deploymentName}')
    if [[ "${deployment_name}" == "" ]]; then
        deployment_name=${dep}
    fi
    wait_deployment_ready ${deployment_name} ${MINAVAILABLE}

    echo "All authentication service pods are ready. Initiating rolling restart."
    for pod in $(kubectl get pods -l component=${dep} -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE})
    do
        echo "Deleting authentication service ${pod}"
        kubectl delete pod ${pod} -n ${ENMASSE_NAMESPACE}
        sleep 30
        wait_deployment_ready ${deployment_name} ${MINAVAILABLE}
    done
done
echo "Authentication Services restarted"

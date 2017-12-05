#!/bin/bash
EXPECTED_PODS=$1
ADDRESS_SPACE=$2

function waitingContainersReady {
    ADDR_SPACE=$1
    pods_id=$(kubectl get pods -n ${ADDR_SPACE} | awk 'NR >1 {print $1}')
    for pod_id in ${pods_id}
    do
        ready=$(kubectl get -o json pod -n ${ADDR_SPACE}  $pod_id -o jsonpath={.status.containerStatuses[0].ready})
        if [ ${ready} == "false" ]
        then
            return 1
        fi
    done
    echo "All containers are ready"
    return 0
}

TIMEOUT=600
NOW=$(date +%s)
END=$(($NOW + $TIMEOUT))
echo "Waiting until $END"
while true
do
    NOW=$(date +%s)
    if [ $NOW -gt $END ]; then
        echo "Timed out waiting for nodes to come up!"
        pods=`kubectl get pods -n ${ADDRESS_SPACE}`
        echo "PODS: $pods"
        exit 1
    fi
    num_running=`kubectl get pods -n ${ADDRESS_SPACE}| grep -v deploy | grep -c Running`
    if [ "$num_running" -eq "$EXPECTED_PODS" ]; then
        waitingContainersReady ${ADDRESS_SPACE}
        if [ $? -gt 0 ]
        then
            echo "All pods are up but all containers are not ready yet"
        else
            echo "ALL UP!"
            exit 0
        fi
    else
        echo "$num_running/$EXPECTED_PODS up"
    fi
    sleep 5
done

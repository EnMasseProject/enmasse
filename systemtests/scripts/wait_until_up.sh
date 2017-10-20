#!/bin/bash
EXPECTED_PODS=$1

function waitingContainersReady {
    pods_id=$(oc get pods | awk 'NR >1 {print $1}')
    for pod_id in ${pods_id}
    do
        ready=$(oc get -o json pod $pod_id -o jsonpath={.status.containerStatuses[0].ready})
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
        pods=`oc get pods`
        echo "PODS: $pods"
        exit 1
    fi
    num_running=`oc get pods | grep -v deploy | grep -c Running`
    if [ "$num_running" -eq "$EXPECTED_PODS" ]; then
        if [ waitingContainersReady ]
        then
            echo "ALL UP!"
            exit 0
        fi
        echo "All pods are up but all containers are not ready yet"
    else
        echo "$num_running/$EXPECTED_PODS up"
    fi
    sleep 5
done

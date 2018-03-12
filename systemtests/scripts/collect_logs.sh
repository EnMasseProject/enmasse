#!/bin/bash
LOGDIR=$1
ARTIFACTS_DIR=$2

function runcmd {
    local cmd=$1
    local logfile=$2
    echo "$cmd > $logfile"
    $cmd > $logfile
}

mkdir -p ${ARTIFACTS_DIR}/logs

for pod in `oc get pods -o jsonpath='{.items[*].metadata.name}'`
do
    for container in `oc get pod $pod -o jsonpath='{.spec.containers[*].name}'`
    do
        runcmd "oc logs -c $container $pod" ${ARTIFACTS_DIR}/logs/${pod}_${container}.log
        if [ "$container" == "router" ]; then
            runcmd "oc rsh -c $container $pod python /usr/bin/qdmanage query --type=address" ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_address.txt
            runcmd "oc rsh -c $container $pod python /usr/bin/qdmanage query --type=connection"  ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_connection.txt
            runcmd "oc rsh -c $container $pod python /usr/bin/qdmanage query --type=connector" ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_connector.txt
        fi
    done
done

cp -r ${LOGDIR}/* ${ARTIFACTS_DIR}/logs/

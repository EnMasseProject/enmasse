#!/bin/bash
function runcmd {
    echo ''
    echo "$1 : "
    $1
    echo ''
    echo '#######################################################################'
}

for pod in `oc get pods -o jsonpath='{.items[*].metadata.name}'`
do
    for container in `oc get pod $pod -o jsonpath='{.spec.containers[*].name}'`
    do
        runcmd "oc logs -c $container $pod"
        if [ "$container" == "router" ]; then
            runcmd "oc rsh -c $container $pod qdmanage query --type=address"
            runcmd "oc rsh -c $container $pod qdmanage query --type=connection"
            runcmd "oc rsh -c $container $pod qdmanage query --type=connector"
        fi
    done
done

for log in `find /tmp/testlogs`
do
    runcmd "cat $log"
done

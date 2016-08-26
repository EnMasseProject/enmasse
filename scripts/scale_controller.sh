#!/bin/sh
ADDRESS=$1
REPLICAS=$2

for i in `seq 1 60`
do
    echo "Checking for replication controller, attempt $i"
    ./oc get replicationcontroller controller-$ADDRESS
    if [ $? -eq 0 ]; then
        echo "Found replication controller, scaling up"
        # ./oc scale replicationcontroller controller-$ADDRESS --replicas=$REPLICAS
        exit 0
    fi
    sleep 5
done
exit 1

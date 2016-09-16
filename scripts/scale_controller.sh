#!/bin/bash
CONTROLLER=$1
REPLICAS=$2

for i in `seq 1 60`
do
    echo "Checking for controller, attempt $i"
    oc get dc $CONTROLLER
    if [ $? -eq 0 ]; then
        echo "Found controller, scaling up"
        oc scale dc $CONTROLLER --replicas=$REPLICAS
        exit 0
    fi
    sleep 5
done
exit 1

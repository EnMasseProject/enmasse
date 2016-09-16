#!/bin/bash
CONTROLLER=$1
REPLICAS=$2

for i in `seq 1 60`
do
    echo "Checking for controller, attempt $i"
    oc get dc $CONTROLLER
    if [ $? -eq 0 ]
    then
        echo "Found controller, scaling up"
        for i in `seq 1 60`
        do
            oc scale dc $CONTROLLER --replicas=$REPLICAS
            if [ $? -eq 0 ]
            then
                exit 0
            else
                echo "Error scaling, retrying in 5 seconds"
                sleep 5
            fi
        done
    fi
    sleep 5
done
exit 1

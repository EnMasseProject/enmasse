#!/bin/sh
EXPECTED_PODS=$1
TIMEOUT=300
NOW=$(date +%s)
END=$(($NOW + $TIMEOUT))
echo "Waiting until $END"
while true
do
    NOW=$(date +%s)
    if [ $NOW -gt $END ]; then
        echo "Timed out waiting for nodes to come up!"
        exit 1
    fi
    num_running=`oc get pods | grep -c Running`
    if [ "$num_running" -eq "$EXPECTED_PODS" ]; then
        echo "ALL UP!"
        exit 0
    else
        echo "$num_running/$EXPECTED_PODS up"
    fi
    sleep 5
done

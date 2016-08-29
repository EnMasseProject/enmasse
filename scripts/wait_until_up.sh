#!/bin/sh
EXPECTED_PODS=$1
while true
do
    num_running=`oc get pods | grep -c Running`
    if [ "$num_running" -eq "$EXPECTED_PODS" ]; then
        echo "ALL UP!"
        exit 0
    else
        echo "$num_running/$EXPECTED_PODS up"
    fi
    sleep 5
done

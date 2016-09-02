#!/bin/sh
for i in `oc get pods | cut -f 1 -d ' ' | grep -v NAME`
do
    echo "LOGS FOR $i"
    oc logs $i 2> /dev/null
    if [ "$?" -gt "0" ]
    then
        oc logs -c broker $i
        oc logs -c router $i
        oc logs -c forwarder $i
    fi
done

# Openshift logs
cat logs/os.err
cat logs/os.log

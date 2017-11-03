#!/bin/bash
SCRIPTDIR=`dirname $0`

oc cluster up $OC_CLUSTER_ARGS
oc login -u system:admin
oc cluster status
while [ $? -gt 0 ]
do
    sleep 5
    oc cluster status
done


sleep 30

# Setup persistent storage
for i in `seq 1 15`
do
    echo "Setup persistent storage ${i}"
    $SCRIPTDIR/provision-storage.sh "/tmp/mydir${i}" "pv-${i}"
done
oc get pv

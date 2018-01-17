#!/bin/bash
SCRIPTDIR=`dirname $0`

sudo rm -rf /var/lib/origin/openshift.local.pv
oc cluster down #for the case that cluster is already running
oc cluster up $OC_CLUSTER_ARGS
oc login -u system:admin
oc cluster status
while [ $? -gt 0 ]
do
    sleep 5
    oc cluster status
done


sleep 30

oc get pv

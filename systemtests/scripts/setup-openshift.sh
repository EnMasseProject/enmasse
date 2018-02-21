#!/bin/bash
SCRIPTDIR=`dirname $0`

oc cluster down #for the case that cluster is already running

oc get status
if [ ! $? -ne 0 ]; then
    echo "ERROR: shutting down of openshift cluster failed, tests won't be executed"
    exit 1
fi

sudo rm -rf /var/lib/origin/openshift.local.pv
sudo rm -rf /var/log/containers/*
sudo rm -rf /var/log/pods/*

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

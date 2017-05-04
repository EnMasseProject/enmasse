#!/bin/bash
set -x
DIR=$1
mkdir -p logs
export MYIP=`ip route get 8.8.8.8 | head -1 | cut -d' ' -f8`
echo "MYIP: $MYIP"
envsubst < master-config.yaml > master.yaml
sudo $DIR/openshift start --master-config=master.yaml 2> logs/os.err > logs/os.log &
sleep 30
sudo chown -R $USER openshift.local.config
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-scc-to-user hostnetwork system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig router
k

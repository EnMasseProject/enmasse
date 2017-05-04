#!/bin/bash
set -x
DIR=$1
mkdir -p logs
sudo $DIR/openshift start --write-config=$DIR 2> logs/os.err > logs/os.log &
ls $DIR

export MYIP=`ip route get 8.8.8.8 | head -1 | cut -d' ' -f8`
echo "MYIP: $MYIP"
sed -i -e "s/router.default.svc.cluster.local/${MYIP}.nip.io/g" $DIR/master-config.yaml
echo "NEW CONFIG: "
cat $DIR/master-config.yaml
sudo $DIR/openshift start --master-config=master-config.yaml 2> logs/os.err > logs/os.log &
sleep 30
sudo chown -R $USER openshift.local.config
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-scc-to-user hostnetwork system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig router
k

#!/bin/bash
set -x
DIR=$1
SDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CDIR=$SDIR/config

mkdir -p logs
mkdir -p $CDIR
sudo $DIR/openshift start --write-config=$CDIR
ls $CDIR

export MYIP=`ip route get 8.8.8.8 | head -1 | cut -d' ' -f8`
echo "MYIP: $MYIP"
sed -i -e "s/router.default.svc.cluster.local/${MYIP}.nip.io/g" $CDIR/master/master-config.yaml
echo "NEW CONFIG: "
cat $CDIR/master/master-config.yaml
sudo $DIR/openshift start --master-config=$CDIR/master/master-config.yaml 2> logs/os.err > logs/os.log &
sleep 30
cat logs/os.err
cat logs/os.log

sudo chown -R $USER openshift.local.config
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-scc-to-user hostnetwork system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
$DIR/oadm --config openshift.local.config/master/admin.kubeconfig router

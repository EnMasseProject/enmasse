#!/bin/bash
set -x
DIR=$1
SDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CDIR=$SDIR/config

mkdir -p $DIR/logs
mkdir -p $CDIR

# Write initial config and make it accessible
pushd $DIR
ls
pwd
sudo $DIR/openshift start --write-config=$CDIR
sudo chown -R $USER $CDIR

MASTER_CONFIG=$CDIR/master/master-config.yaml
NODE_CONFIG=`ls $CDIR/node*/node-config.yaml`

echo "Master config: $MASTER_CONFIG"
echo "Node config: $NODE_CONFIG"

# Replace with build node ip to get proper subdomain routing
MYIP=`ip route get 8.8.8.8 | head -1 | cut -d' ' -f8`
echo "MYIP: $MYIP"
sed -i -e "s/router.default.svc.cluster.local/${MYIP}.nip.io/g" $MASTER_CONFIG

# Start OpenShift with config
sudo $DIR/openshift start --master-config=$MASTER_CONFIG --node-config=$NODE_CONFIG 2> $DIR/logs/os.err > $DIR/logs/os.log &
popd
sleep 30

# Deploy HAProxy router
$DIR/oadm --config $CDIR/master/admin.kubeconfig policy add-scc-to-user hostnetwork system:serviceaccount:default:router
$DIR/oadm --config $CDIR/master/admin.kubeconfig policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
$DIR/oadm --config $CDIR/master/admin.kubeconfig router

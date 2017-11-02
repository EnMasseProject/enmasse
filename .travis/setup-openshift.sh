#!/bin/sh
set -x
BASE=$1

SETUP=$BASE/setup
LOGDIR=$BASE/logs
CONFIG=$BASE/config

mkdir -p $SETUP
mkdir -p $LOGDIR
mkdir -p $CONFIG

sudo service docker stop
sudo sh -c 'echo DOCKER_OPTS=\"-H tcp://127.0.0.1:2375 -H unix:///var/run/docker.sock --insecure-registry 172.30.0.0/16\" > /etc/default/docker'
sudo cat /etc/default/docker
sudo service docker start

wget https://github.com/openshift/origin/releases/download/v1.5.1/openshift-origin-server-v1.5.1-7b451fc-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $SETUP --strip-components 1

sudo cp $SETUP/* /usr/bin

sudo openshift start --write-config=$CONFIG
sudo chown -R $USER $CONFIG

MASTER_CONFIG=$CONFIG/master/master-config.yaml
NODE_CONFIG=`ls $CONFIG/node*/node-config.yaml`

echo "Master config: $MASTER_CONFIG"
echo "Node config: $NODE_CONFIG"

# Replace with build node ip to get proper subdomain routing
MYIP=`ip route get 8.8.8.8 | head -1 | cut -d' ' -f8`
echo "Using IP: $MYIP"
sed -i -e "s/router.default.svc.cluster.local/${MYIP}.nip.io/g" $MASTER_CONFIG

# Start OpenShift with config
sudo openshift start --master-config=$MASTER_CONFIG --node-config=$NODE_CONFIG 2> $LOGDIR/os.err > $LOGDIR/os.log &
echo "Waiting for OpenShift to start"
sleep 60

KUBECONFIG=$CONFIG/master/admin.kubeconfig

# Deploy HAProxy router
oc adm --config $KUBECONFIG policy add-scc-to-user hostnetwork system:serviceaccount:default:router
oc adm --config $KUBECONFIG policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
oc adm --config $KUBECONFIG router

# Deploy docker registry
oc adm --config $KUBECONFIG registry -o json | sed -e 's/"sessionAffinity"/"clusterIP": "172.30.1.1","sessionAffinity"/g' > $CONFIG/registry.json
oc create --config $KUBECONFIG -f $CONFIG/registry.json

sleep 30
oc --config $KUBECONFIG get services -n default
oc --config $KUBECONFIG get pods -n default

# Setup persistent storage
export KUBECONFIG
for i in `seq 1 15`
do
    echo "Setup persistent storage ${i}"
    ./systemtests/scripts/provision-storage.sh "/tmp/mydir${i}" "pv-${i}"
done
oc get pv

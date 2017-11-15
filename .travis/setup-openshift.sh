#!/bin/sh
set -x
BASE=$1

SETUP=$BASE/setup
LOGDIR=$BASE/logs
CONFIG=$BASE/config

mkdir -p $SETUP
mkdir -p $LOGDIR
mkdir -p $CONFIG

#sudo apt-get update -y
#sudo apt-get install -y systemd-services

sudo service docker stop
sudo sh -c 'echo DOCKER_OPTS=\"-H tcp://127.0.0.1:2375 -H unix:///var/run/docker.sock --insecure-registry 172.30.0.0/16\" > /etc/default/docker'
sudo cat /etc/default/docker
sudo mount --make-shared /
sudo service docker start

wget -q https://github.com/openshift/origin/releases/download/v3.7.0-rc.0/openshift-origin-client-tools-v3.7.0-rc.0-e92d5c5-linux-64bit.tar.gz -O openshift.tar.gz
# wget https://github.com/openshift/origin/releases/download/v1.5.1/openshift-origin-server-v1.5.1-7b451fc-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $SETUP --strip-components 1

sudo cp $SETUP/* /usr/bin

oc cluster up
sudo chown -R $USER /var/lib/origin/openshift.local.config
sudo chmod -R 777 /var/lib/origin/openshift.local.pv

#sudo openshift start --write-config=$CONFIG
#
#MASTER_CONFIG=$CONFIG/master/master-config.yaml
#NODE_CONFIG=`ls $CONFIG/node*/node-config.yaml`

#echo "Master config: $MASTER_CONFIG"
#echo "Node config: $NODE_CONFIG"
#
## Replace with build node ip to get proper subdomain routing
#sed -i -e "s/router.default.svc.cluster.local/${MYIP}.nip.io/g" $MASTER_CONFIG
#
## Start OpenShift with config
#sudo openshift start --master-config=$MASTER_CONFIG --node-config=$NODE_CONFIG 2> $LOGDIR/os.err > $LOGDIR/os.log &
#echo "Waiting for OpenShift to start"
#sleep 60
#
#KUBECONFIG=$CONFIG/master/admin.kubeconfig
#
## Deploy HAProxy router
#oc adm --config $KUBECONFIG policy add-scc-to-user hostnetwork system:serviceaccount:default:router
#oc adm --config $KUBECONFIG policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:router
#oc adm --config $KUBECONFIG router
#
## Deploy docker registry
#oc adm --config $KUBECONFIG registry -o json | sed -e 's/"sessionAffinity"/"clusterIP": "172.30.1.1","sessionAffinity"/g' > $CONFIG/registry.json
#oc create --config $KUBECONFIG -f $CONFIG/registry.json

oc login -u system:admin

NOW=$(date +%s)
TIMEOUT=300
END=$(($NOW + $TIMEOUT))
oc cluster status
while [ $? -gt 0 ]
do
    NOW=$(date +%s)
    if [ $NOW -gt $END ]; then
        echo "Timed out waiting for OpenShift to start!"
        exit 1
    fi
    sleep 5
    oc get nodes -o yaml
    oc cluster status
done

oc get services -n default
oc get pods -n default
oc get pv

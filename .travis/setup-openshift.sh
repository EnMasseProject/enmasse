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
sudo mount --make-shared /
sudo service docker start

wget -q https://github.com/openshift/origin/releases/download/v3.6.0/openshift-origin-client-tools-v3.6.0-c4dd4cf-linux-64bit.tar.gz -O openshift.tar.gz

tar xzf openshift.tar.gz -C $SETUP --strip-components 1

sudo cp $SETUP/oc /usr/bin
oc cluster up

oc login -u system:admin
oc cluster status
while [ $? -gt 0 ]
do
    sleep 5
    oc cluster status
done

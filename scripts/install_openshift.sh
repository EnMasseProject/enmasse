#!/bin/bash
DEST=$1
mkdir -p $DEST
wget https://github.com/openshift/origin/releases/download/v1.4.1/openshift-origin-client-tools-v1.4.1-3f9807a-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $DEST --strip-components 1

sudo sh -c 'echo DOCKER_OPTS="-H tcp://127.0.0.1:2375 -H unix:///var/run/docker.sock --insecure-registry 172.30.0.0/16" > /etc/default/docker'
sudo service docker restart
sudo cat /etc/default/docker

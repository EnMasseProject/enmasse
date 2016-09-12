#!/bin/sh
DEST=$1
mkdir -p $DEST
wget https://github.com/openshift/origin/releases/download/v1.3.0-rc1/openshift-origin-server-v1.3.0-rc1-ac0bb1bf6a629e0c262f04636b8cf2916b16098c-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $DEST --strip-components 1

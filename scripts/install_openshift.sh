#!/bin/sh
DEST=$1
mkdir -p $DEST
wget https://github.com/openshift/origin/releases/download/v1.3.0-alpha.3/openshift-origin-server-v1.3.0-alpha.3-7998ae4-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $DEST --strip-components 1

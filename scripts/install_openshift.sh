#!/bin/bash
DEST=$1
mkdir -p $DEST
wget https://github.com/openshift/origin/releases/download/v1.3.2/openshift-origin-server-v1.3.2-ac1d579-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $DEST --strip-components 1

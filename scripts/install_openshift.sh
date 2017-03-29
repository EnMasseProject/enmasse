#!/bin/bash
DEST=$1
mkdir -p $DEST
wget https://github.com/openshift/origin/releases/download/v1.4.1/openshift-origin-client-tools-v1.4.1-3f9807a-linux-64bit.tar.gz -O openshift.tar.gz
tar xzf openshift.tar.gz -C $DEST --strip-components 1

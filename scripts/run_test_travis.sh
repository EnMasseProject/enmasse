#!/bin/bash
DIR=`dirname $0`
set -x
source $DIR/common.sh

curl https://dl.bintray.com/enmasse/snapshots/latest/enmasse-latest.tar.gz -o $DIR/enmasse-latest.tar.gz
tar xzvf $DIR/enmasse-latest.tar.gz
export PATH="$PATH:$DIR/enmasse-latest"

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

setup_test enmasse-ci
run_test enmasse-ci true || failure=$(($failure + 1))
# teardown_test enmasse-ci

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

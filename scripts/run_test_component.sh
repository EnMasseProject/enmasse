#!/bin/bash
ENMASSE_DIR=$1
DIR=`dirname $0`
set -x
source $DIR/common.sh
failure=0

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

setup_test enmasse-ci $ENMASSE_DIR
run_test enmasse-ci true || failure=$(($failure + 1))
# teardown_test enmasse-ci

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

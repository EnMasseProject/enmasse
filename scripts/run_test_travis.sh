#!/bin/bash
DIR=`dirname $0`
set -x
source $DIR/common.sh

curl https://raw.githubusercontent.com/EnMasseProject/enmasse/master/scripts/enmasse-deploy.sh -o $DIR/enmasse-deploy.sh
chmod 755 $DIR/enmasse-deploy.sh
export PATH="$PATH:$DIR"

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

setup_test enmasse-ci true
run_test enmasse-ci true || failure=$(($failure + 1))
# teardown_test enmasse-ci

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

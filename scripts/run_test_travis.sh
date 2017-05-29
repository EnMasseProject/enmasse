#!/bin/bash
DIR=`dirname $0`
set -x
source $DIR/common.sh

curl https://raw.githubusercontent.com/EnMasseProject/enmasse/master/scripts/deploy-openshift.sh -o $DIR/enmasse-deploy.sh
curl https://raw.githubusercontent.com/EnMasseProject/enmasse/master/install/openshift/enmasse.yaml -o $DIR/enmasse.yaml
chmod 755 $DIR/enmasse-deploy.sh
export PATH="$PATH:$DIR"

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

setup_test enmasse-ci $DIR/enmasse.yaml
run_test enmasse-ci true || failure=$(($failure + 1))
# teardown_test enmasse-ci

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

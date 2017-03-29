#!/bin/bash
DIR=`dirname $0`
set -x

function setup_test() {
    PROJECT_NAME=$1

    ./enmasse-deploy.sh -p $PROJECT_NAME -u test -c https://localhost:8443
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}


function setup_test_multitenant() {
    PROJECT_NAME=$1
    ./enmasse-deploy.sh -p $PROJECT_NAME -u test -c https://localhost:8443 -m

    sudo ./openshift/oadm --config openshift.local.config/master/admin.kubeconfig policy add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account
    sudo ./openshift/oadm --config openshift.local.config/master/admin.kubeconfig policy add-cluster-role-to-user cluster-admin test
}

function setup_test_secure() {
    PROJECT_NAME=$1
    openssl req -x509 -newkey rsa:4096 -keyout server-key.pem -out server-cert.pem -days 1 -nodes -batch
    ./enmasse-deploy.sh -p $PROJECT_NAME -u test -c https://localhost:8443 -k server-key.pem -s server-cert.pem
    export OPENSHIFT_USE_TLS="true"
    export OPENSHIFT_SERVER_CERT=`cat server-cert.pem`
}

function run_test() {
    PROJECT_NAME=$1
    USE_TLS=$2
    MULTIINSTANCE=$3

    if [ "$MULTIINSTANCE" == false ]; then
        $DIR/wait_until_up.sh 6 || exit 1
    else
        $DIR/wait_until_up.sh 1 || exit 1
    fi

    sleep 120

    OPENSHIFT_USE_TLS=$USE_TLS OPENSHIFT_NAMESPACE=$PROJECT_NAME OPENSHIFT_USER=test OPENSHIFT_MULTITENANT=$MULTIINSTANCE OPENSHIFT_TOKEN=`oc config view -o jsonpath='{.users[?(@.name == "test/localhost:8443")].user.token}'` OPENSHIFT_MASTER_URL=https://localhost:8443 gradle check -i --rerun-tasks -Djava.net.preferIPv4Stack=true $ARGS
}

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443
curl https://raw.githubusercontent.com/EnMasseProject/enmasse/master/scripts/enmasse-deploy.sh -o enmasse-deploy.sh
chmod 755 enmasse-deploy.sh

failure=0
setup_test enmasse-ci-single
run_test enmasse-ci-single false false || failure=$(($failure + 1))
teardown_test enmasse-ci-single

#setup_test_multitenant enmasse-ci-multi
#run_test enmasse-ci-multi false true || failure=$(($failure + 1))
#teardown_test enmasse-ci-multi
#
#setup_test_secure enmasse-ci-secure
#run_test enmasse-ci-secure true false || failure=$(($failure + 1))
#teardown_test enmasse-ci-secure

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

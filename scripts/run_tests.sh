#!/bin/bash
DIR=`dirname $0`
set -x

function setup_test() {
    PROJECT_NAME=$1
    oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

    oc new-project $PROJECT_NAME
    oc create sa enmasse-service-account -n $(oc project -q)
    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
    oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):enmasse-service-account
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}


function setup_test_multitenant() {
    PROJECT_NAME=$1

    oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443
    oc new-project $PROJECT_NAME
    oc create sa enmasse-service-account -n $(oc project -q)

    oc login -u system:admin --insecure-skip-tls-verify=true https://localhost:8443
    oadm policy add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account

    oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443
}

function setup_secure() {
    openssl req -x509 -newkey rsa:4096 -keyout server-key.pem -out server-cert.pem -days 1 -nodes -batch
    oc secret new qdrouterd-certs server-cert.pem server-key.pem
    oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount
    export OPENSHIFT_USE_TLS="true"
    export OPENSHIFT_SERVER_CERT=`cat server-cert.pem`
}

function run_test() {
    PROJECT_NAME=$1
    USE_TLS=$2
    MULTIINSTANCE=$3
    TEMPLATE=$4

    oc process -f $TEMPLATE MULTIINSTANCE=$MULTIINSTANCE | oc create -f -

    if [ "$MULTIINSTANCE" == false ]; then
        $DIR/wait_until_up.sh 6 || exit 1
    else
        $DIR/wait_until_up.sh 1 || exit 1
    fi

    sleep 120

    OPENSHIFT_USE_TLS=$USE_TLS OPENSHIFT_NAMESPACE=$PROJECT_NAME OPENSHIFT_USER=test OPENSHIFT_MULTITENANT=$MULTIINSTANCE OPENSHIFT_TOKEN=`oc config view -o jsonpath='{.users[?(@.name == "test/localhost:8443")].user.token}'` OPENSHIFT_MASTER_URL=https://localhost:8443 gradle check -i --rerun-tasks -Djava.net.preferIPv4Stack=true
}

failure=0
setup_test enmasse-ci-single
run_test enmasse-ci-single false false https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/generated/enmasse-template.yaml || failure=$(($failure + 1))
teardown_test enmasse-ci-single

setup_test enmasse-ci-multi
run_test enmasse-ci-multi false true https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/generated/enmasse-template.yaml || failure=$(($failure + 1))
teardown_test enmasse-ci-multi

#setup_test enmasse-ci-secure
#setup_secure
#run_test enmasse-ci-secure true false https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/generated/tls-enmasse-template.yaml || exit 1


if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi

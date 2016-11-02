#!/bin/bash
DIR=`dirname $0`
set -x

oc login -u test -p test --insecure-skip-tls-verify=true https://localhost:8443

function setup_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME

    oc new-project $PROJECT_NAME
    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
    oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):deployer
}

function setup_secure() {
    openssl req -x509 -newkey rsa:4096 -keyout server-key.pem -out server-cert.pem -days 1 -nodes -batch
    oc secret new qdrouterd-certs server-cert.pem server-key.pem
    oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount
export OPENSHIFT_USE_TLS="true"
}

function run_test() {
    PROJECT_NAME=$1
    USE_TLS=$2
    TEMPLATE=$3

    oc process -f $TEMPLATE | oc create -f -

    $DIR/wait_until_up.sh 7 || exit 1

    sleep 120

    OPENSHIFT_USE_TLS=$USE_TLS OPENSHIFT_NAMESPACE=$PROJECT_NAME OPENSHIFT_USER=test OPENSHIFT_TOKEN=`oc config view -o jsonpath='{.users[?(@.name == "test/localhost:8443")].user.token}'` OPENSHIFT_URL=https://localhost:8443 gradle check -i --rerun-tasks
}

setup_test enmasse-ci-default
run_test enmasse-ci-default false https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/generated/enmasse-template.yaml || exit 1

setup_test enmasse-ci-secure
setup_secure
run_test enmasse-ci-secure true https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/generated/tls-enmasse-template.yaml || exit 1

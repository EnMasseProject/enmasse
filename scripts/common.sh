#!/bin/bash
DIR=`dirname $0`
set -x

OADM="oadm --config openshift.local.config/master/admin.kubeconfig"

function setup_test() {
    local PROJECT_NAME=$1
    local SECURE=${2:-false}
    local MULTITENANT=${3:-false}
    local OPENSHIFT_HOST=${4:-"https://localhost:8443"}
    local OPENSHIFT_USER=${5:-"test"}

    DEPLOY_ARGS="-y -p $PROJECT_NAME -u $OPENSHIFT_USER -c $OPENSHIFT_HOST"
    if [ "$SECURE" == true ]; then
        openssl req -x509 -newkey rsa:4096 -keyout server-key.pem -out server-cert.pem -days 1 -nodes -batch
        DEPLOY_ARGS="$DEPLOY_ARGS -k server-key.pem -s server-cert.pem"
        export OPENSHIFT_USE_TLS="true"
        export OPENSHIFT_SERVER_CERT=`cat server-cert.pem`
    fi

    if [ "$MULTITENANT" == true ]; then
        DEPLOY_ARGS="$DEPLOY_ARGS -m"
    fi

    enmasse-deploy.sh $DEPLOY_ARGS

    if [ "$MULTITENANT" == true ]; then
        $OADM add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account
        $OADM policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    fi
}

function run_test() {
    local PROJECT_NAME=$1
    local SECURE=${2:-false}
    local MULTITENANT=${3:-false}
    local OPENSHIFT_URL=${4:-"https://localhost:8443"}
    local OPENSHIFT_USER=${5:-"test"}

    if [ "$MULTITENANT" == false ]; then
        $DIR/wait_until_up.sh 6 || exit 1
    else
        $DIR/wait_until_up.sh 1 || exit 1
    fi

    sleep 120

    export OPENSHIFT_NAMESPACE=$PROJECT_NAME
    export OPENSHIFT_USER=$OPENSHIFT_USER
    export OPENSHIFT_MULTITENANT=$MULTITENANT
    export OPENSHIFT_TOKEN=`oc whoami -t`
    export OPENSHIFT_MASTER_URL=$OPENSHIFT_HOST
    gradle check -i --rerun-tasks -Djava.net.preferIPv4Stack=true
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}


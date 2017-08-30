#!/bin/bash
DIR=`readlink -f \`dirname $0\``
set -x

OADM="oc adm --config openshift.local.config/master/admin.kubeconfig"

function download_enmasse() {
    curl -0 https://dl.bintray.com/enmasse/snapshots/latest/enmasse-latest.tar.gz | tar -zx
    D=`readlink -f enmasse-latest`
    echo $D
}

function setup_test() {
    local PROJECT_NAME=$1
    local ENMASSE_DIR=$2
    local MULTITENANT=${3:-false}
    local OPENSHIFT_URL=${4:-"https://localhost:8443"}
    local OPENSHIFT_USER=${5:-"test"}

    DEPLOY_ARGS="-y -n $PROJECT_NAME -u $OPENSHIFT_USER -m $OPENSHIFT_URL"

    if [ "$MULTITENANT" == true ]; then
        DEPLOY_ARGS="$DEPLOY_ARGS -p MULTIINSTANCE=true"
    fi

    $ENMASSE_DIR/deploy-openshift.sh $DEPLOY_ARGS

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
        $DIR/wait_until_up.sh 7 || return 1
    else
        $DIR/wait_until_up.sh 4 || return 1
    fi

    sleep 120

    export OPENSHIFT_USE_TLS=$SECURE
    export OPENSHIFT_NAMESPACE=$PROJECT_NAME
    export OPENSHIFT_USER=$OPENSHIFT_USER
    export OPENSHIFT_MULTITENANT=$MULTITENANT
    export OPENSHIFT_TOKEN=`oc whoami -t`
    export OPENSHIFT_MASTER_URL=$OPENSHIFT_URL
    ../gradlew :systemtests:test -Psystemtests -i --rerun-tasks -Djava.net.preferIPv4Stack=true
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}


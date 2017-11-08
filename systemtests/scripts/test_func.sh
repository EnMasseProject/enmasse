#!/usr/bin/env bash

function download_enmasse() {

    D=`readlink -f enmasse-latest`
    echo $D
}

function setup_test() {
    export OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
    export OPENSHIFT_USER=${OPENSHIFT_USER:-test}
    export OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
    export OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
    export OPENSHIFT_MULTITENANT=${OPENSHIFT_MULTITENANT:-true}
    export OPENSHIFT_TEST_LOGDIR=${OPENSHIFT_TEST_LOGDIR:-/tmp/testlogs}
    export OPENSHIFT_USE_TLS=${OPENSHIFT_USE_TLS:-true}
    export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
    export CURDIR=`readlink -f \`dirname $0\``
    export DEFAULT_AUTHSERVICE=standard

    oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}
    oc project ${OPENSHIFT_PROJECT}

    export OPENSHIFT_TOKEN=`oc whoami -t`
    rm -rf $OPENSHIFT_TEST_LOGDIR
    mkdir -p $OPENSHIFT_TEST_LOGDIR

    DEPLOY_ARGS=( "-y" "-n" "$OPENSHIFT_PROJECT" "-u" "$OPENSHIFT_USER" "-m" "$OPENSHIFT_URL" "-a" "none standard" )
    if [ "$OPENSHIFT_MULTITENANT" == true ]; then
        DEPLOY_ARGS+=( "-o" "multi" )
    fi

    $ENMASSE_DIR/deploy-openshift.sh "${DEPLOY_ARGS[@]}"

    if [ "$OPENSHIFT_MULTITENANT" == "true" ]; then
        oc adm --config $KUBEADM policy add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account
        oc adm --config $KUBEADM policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    fi
}

function run_test() {
    if [ "$OPENSHIFT_MULTITENANT" == false ]; then
        $CURDIR/wait_until_up.sh 9 || return 1
    else
        $CURDIR/wait_until_up.sh 4 || return 1
    fi
    # Run a single test case
    if [ -n "$TESTCASE" ]; then
        EXTRA_TEST_ARGS="-Dtest=$TESTCASE"
    fi
    mvn test -pl systemtests -Psystemtests -Djava.net.preferIPv4Stack=true $EXTRA_TEST_ARGS
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}

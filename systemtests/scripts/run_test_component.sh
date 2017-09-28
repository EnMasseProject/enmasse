#!/bin/bash
ENMASSE_DIR=$1
KUBEADM=$2
SYSTEMTESTS=$3

function download_enmasse() {
    curl -0 https://dl.bintray.com/enmasse/snapshots/latest/enmasse-latest.tar.gz | tar -zx
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
    mkdir -p $OPENSHIFT_TEST_LOGDIR

    DEPLOY_ARGS=( "-y" "-n" "$OPENSHIFT_PROJECT" "-u" "$OPENSHIFT_USER" "-m" "$OPENSHIFT_URL" "-a" "none standard" )
    if [ "$OPENSHIFT_MULTITENANT" == true ]; then
        DEPLOY_ARGS+=( "-p" "MULTITENANT=true" )
    fi

    $ENMASSE_DIR/deploy-openshift.sh "${DEPLOY_ARGS[@]}"

    if [ "$OPENSHIFT_MULTITENANT" == "true" ]; then
        oc adm --config $KUBEADM policy add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account
        oc adm --config $KUBEADM policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    fi
}

function run_test() {
    if [ "$OPENSHIFT_MULTITENANT" == false ]; then
        $CURDIR/wait_until_up.sh 7 || return 1
    else
        $CURDIR/wait_until_up.sh 4 || return 1
    fi
    ../gradlew :systemtests:test -Psystemtests -i --rerun-tasks -Djava.net.preferIPv4Stack=true
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}


failure=0

setup_test

pushd $SYSTEMTESTS
run_test || failure=$(($failure + 1))
popd

$CURDIR/collect_logs.sh $ARTIFACTS_DIR

oc get pods

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test $OPENSHIFT_PROJECT
fi

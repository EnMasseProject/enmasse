#!/usr/bin/env bash

function download_enmasse() {
    curl -0 https://dl.bintray.com/enmasse/snapshots/enmasse-latest.tgz | tar -zx
    D=`readlink -f enmasse-latest`
    echo $D
}

function setup_test() {
    ENMASSE_DIR=$1
    KUBEADM=$2

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

    ${ENMASSE_DIR}/deploy-openshift.sh "${DEPLOY_ARGS[@]}"

    if [ "$OPENSHIFT_MULTITENANT" == "true" ]; then
        oc adm --config ${KUBEADM} policy add-cluster-role-to-user cluster-admin system:serviceaccount:$(oc project -q):enmasse-service-account
        oc adm --config ${KUBEADM} policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    fi
}
function wait_until_up(){
    POD_COUNT=$1
    ADDR_SPACE=$2
    $CURDIR/wait_until_up.sh ${POD_COUNT} ${ADDR_SPACE} || return 1
}

function run_test() {
    TESTCASE=$1
    TEST_PROFILE=${2:-'systemtests'}
    if [ "$OPENSHIFT_MULTITENANT" == false ]; then
        wait_until_up 9 ${OPENSHIFT_PROJECT} || return 1
    else
        wait_until_up 4 ${OPENSHIFT_PROJECT} || return 1
    fi
    # Run a single test case
    if [ -n "${TESTCASE}" ]; then
        EXTRA_TEST_ARGS="-Dtest=${TESTCASE}"
    fi
    mvn test -pl systemtests -P${TEST_PROFILE} -Djava.net.preferIPv4Stack=true ${EXTRA_TEST_ARGS}
}

function teardown_test() {
    PROJECT_NAME=$1
    oc delete project $PROJECT_NAME
}

function create_addres_space() {
    ADDRESS_SPACE_NAME=$1
    ADDRESS_SPACE_DEF=$2
    TOKEN=$(oc whoami -t)
    curl -X POST -H "content-type: application/json" --data-binary @${ADDRESS_SPACE_DEF} -H "Authorization: Bearer ${TOKEN}" http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v1/addressspaces
    wait_until_up 2 ${ADDRESS_SPACE_NAME} || return 1
}

function create_addresses() {
    ADDRESS_SPACE_NAME=$1
    ADDRESSES_DEF=$2
    TOKEN=$(oc whoami -t)
    curl -X PUT -H "content-type: application/json" --data-binary @${ADDRESSES_DEF} -H "Authorization: Bearer ${TOKEN}" http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v1/addresses/${ADDRESS_SPACE_NAME}
    sleep 40 #waiting for addresses are ready
}

function create_user() {
    CLI_ID=$1
    NEW_USER_DEF=$2
    ADDRESS_SPACE_NAME=$3

    #get keycloak credentials
    oc extract secret/keycloak-credentials
    USER=$(cat admin.username)
    PASSWORD=$(cat admin.password)

    # get token
    RESULT=$(curl --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" http://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
    TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

    #create user
    curl -X POST -H "content-type: application/json" --data-binary @${NEW_USER_DEF} -H "Authorization: Bearer ${TOKEN}"  http://${USER}:${PASSWORD}@$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users
}

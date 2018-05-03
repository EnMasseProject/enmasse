#!/usr/bin/env bash
CURDIR="$(readlink -f $(dirname $0))"

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
    export OPENSHIFT_TEST_LOGDIR=${OPENSHIFT_TEST_LOGDIR:-/tmp/testlogs}
    export OPENSHIFT_USE_TLS=${OPENSHIFT_USE_TLS:-true}
    export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
    export CURDIR=`readlink -f \`dirname $0\``
    export DEFAULT_AUTHSERVICE=standard

    rm -rf $OPENSHIFT_TEST_LOGDIR
    mkdir -p $OPENSHIFT_TEST_LOGDIR

    oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}
    oc adm --config ${KUBEADM} policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    export OPENSHIFT_TOKEN=`oc whoami -t`
    ansible-playbook ${ENMASSE_DIR}/ansible/playbooks/openshift/systemtests.yml --extra-vars "namespace=${OPENSHIFT_PROJECT} admin_user=${OPENSHIFT_USER}"
}

function wait_until_up(){
    POD_COUNT=$1
    ADDR_SPACE=$2
    ${CURDIR}/wait_until_up.sh ${POD_COUNT} ${ADDR_SPACE} || return 1
}

function run_test() {
    TESTCASE=$1
    PROFILE=${2:-systemtests}
    CLUSTER_TYPE=${3:-openshift}

    expected_pods=5
    if [ "$CLUSTER_TYPE" == "kubernetes" ]; then
        expected_pods=4
    fi
    wait_until_up ${expected_pods} ${OPENSHIFT_PROJECT}
    wait_code=$?
    if [ $wait_code -ne 0 ]; then
        echo "SYSTEM-TESTS WILL BE NOT EXECUTED (TESTCASE=${TESTCASE}; PROFILE=${PROFILE})"
        return 1
    fi
    # Run a single test case
    if [ -n "${TESTCASE}" ]; then
        EXTRA_TEST_ARGS="-Dtest=${TESTCASE}"
    fi
    mvn -B test -pl systemtests -P${PROFILE} -Djava.net.preferIPv4Stack=true -DfailIfNoTests=false ${EXTRA_TEST_ARGS}
}

function teardown_test() {
    PROJECT_NAME=$1
    kubectl delete namespace $PROJECT_NAME
}

function create_addres_space() {
    ADDRESS_SPACE_NAME=$1
    ADDRESS_SPACE_DEF=$2
    TOKEN=$(oc whoami -t)
    curl -k -X POST -H "content-type: application/json" --data-binary @${ADDRESS_SPACE_DEF} -H "Authorization: Bearer ${TOKEN}" https://$(oc get route -o jsonpath='{.spec.host}' restapi)/apis/enmasse.io/v1/addressspaces
    wait_until_up 2 ${ADDRESS_SPACE_NAME} || return 1
}

function create_addresses() {
    ADDRESS_SPACE_NAME=$1
    ADDRESSES_DEF=$2
    TOKEN=$(oc whoami -t)
    curl -k -X PUT -H "content-type: application/json" --data-binary @${ADDRESSES_DEF} -H "Authorization: Bearer ${TOKEN}" https://$(oc get route -o jsonpath='{.spec.host}' restapi)/apis/enmasse.io/v1/addresses/${ADDRESS_SPACE_NAME}
    sleep 40 #waiting for addresses are ready
}

function create_user() {
    CLI_ID=$1
    USER=$2
    PASSWORD=$3
    ADDRESS_SPACE_NAME=$4
    NEW_USER_DEF=$5

    # get token
    RESULT=$(curl -k --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
    TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

    #create user
    curl -k -X POST -H "content-type: application/json" --data-binary @${NEW_USER_DEF} -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users
}

function join_group() {
    CLI_ID=$1
    USER=$2
    PASSWORD=$3
    ADDRESS_SPACE_NAME=$4
    USER_NAME=$5
    GROUP_NAME=$6


    # get token
    RESULT=$(curl -k --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
    TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

    #GET USER ID
    echo "get user id: ${USER_NAME}"
    TCKUSERJSON=$(curl -k -X GET -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users?search=${USER_NAME})
    TCK_USER_ID=$(echo ${TCKUSERJSON} | jq -r '.[].id')
    echo "user id: ${TCK_USER_ID}"

    #GET GROUP ID
    echo "get group id: ${GROUP_NAME}"
    KEYCLOAK_GROUPS=$(curl -k -X GET -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/groups?search=${GROUP_NAME})
    GROUP_ID=$(echo ${KEYCLOAK_GROUPS} | jq -r '.[].id')
    echo "group id: ${GROUP_ID}"

    #JOIN GROUP
    $(curl -k -X PUT -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users/${TCK_USER_ID}/groups/${GROUP_ID})
}

function create_group() {
    CLI_ID=$1
    USER=$2
    PASSWORD=$3
    ADDRESS_SPACE_NAME=$4
    GROUP_NAME=$5

    GROUP_DEF="{\"name\":\"${GROUP_NAME}\"}"
    echo ${GROUP_DEF}

    # get token
    RESULT=$(curl -k --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
    TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

    #create group
    curl -k -X POST -H "content-type: application/json" -d ${GROUP_DEF} -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/groups/
}

function get_kubernetes_info() {
    LOG_DIR=${1}
    RESOURCE=${2}
    NAMESPACE=${3}
    SUFIX=${4}
    FILE_NAME="openshift-${RESOURCE}-${NAMESPACE}${SUFIX}.log"
    kubectl get ${RESOURCE} -n ${NAMESPACE} > "${LOG_DIR}/${FILE_NAME}"
    kubectl get ${RESOURCE} -n ${NAMESPACE} -o yaml >> "${LOG_DIR}/${FILE_NAME}"
}

#store previous logs from restarted pods
function get_previous_logs() {
    LOG_DIR=${1}
    ADDRESS_SPACE=${2}

    pods_id=$(kubectl get pods -n ${ADDRESS_SPACE} | awk 'NR >1 {print $1}')
    for pod_id in ${pods_id}
    do
        restart_count=$(kubectl get -o json pod -n ${ADDRESS_SPACE} ${pod_id} -o jsonpath={.status.containerStatuses[0].restartCount})
        if (( ${restart_count} > 0 ))
        then
            echo "pod ${pod_id} was restarted"
            kubectl logs -p ${pod_id} -n ${ADDRESS_SPACE} > "${LOG_DIR}/${pod_id}.previous.log"
        fi
    done
}

function get_all_events() {
    LOG_DIR=${1}
    kubectl get events --all-namespaces > ${LOG_DIR}/all_events.log
}

function get_docker_info() {
    ARTIFACTS_DIR=${1}
    CONTAINER=${2}

    FILENAME_STDOUT="docker_${CONTAINER}.stdout.log"
    FILENAME_STDERR="docker_${CONTAINER}.stderr.log"
    docker logs ${CONTAINER} > ${FILENAME_STDOUT} 2> ${FILENAME_STDERR}
}

function categorize_dockerlogs {
    LOG_DIR=${1}
    for x in ${LOG_DIR}/*; do
        ADDR_SPACE="$(echo "${x}" | sed -e 's/^[^_]\+_\([^_]\+\)_[^_]\+\.log$/\1/')";
        if [[ ! -d "${LOG_DIR}/${ADDR_SPACE}" ]]; then
            mkdir "${LOG_DIR}/${ADDR_SPACE}";
        fi
        mv "${x}" "${LOG_DIR}/${ADDR_SPACE}"
    done
}

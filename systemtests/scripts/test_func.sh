#!/usr/bin/env bash
CURDIR="$(readlink -f $(dirname $0))"

source "${CURDIR}/../../scripts/logger.sh"

function download_enmasse() {
    curl -0 https://dl.bintray.com/enmasse/snapshots/enmasse-latest.tgz | tar -zx
    D=`readlink -f enmasse-latest`
    echo $D
}

function setup_test() {
    TEMPLATES_INSTALL_DIR=$1
    KUBEADM=$2
    REG_API_SERVER=${3:-true}
    SKIP_DEPENDENCIES=${4:-false}
    UPGRADE=${5:-false}

    export_required_env
    export REGISTER_API_SERVER=${REG_API_SERVER}

    info "Deploying enmasse with templates dir: ${TEMPLATES_INSTALL_DIR}, kubeadmin: ${KUBEADM}, skip setup: ${SKIP_DEPENDENCIES}"

    rm -rf $OPENSHIFT_TEST_LOGDIR
    mkdir -p $OPENSHIFT_TEST_LOGDIR

    oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}
    oc adm --config ${KUBEADM} policy add-cluster-role-to-user cluster-admin $OPENSHIFT_USER
    export OPENSHIFT_TOKEN=`oc whoami -t`

    if [[ "${SKIP_DEPENDENCIES}" == "false" ]]; then
        ansible-playbook ${CURDIR}/../ansible/playbooks/systemtests-dependencies.yml
    fi
    ansible-playbook ${TEMPLATES_INSTALL_DIR}/ansible/playbooks/openshift/deploy_all.yml -i ${CURDIR}/../ansible/inventory/systemtests.inventory --extra-vars "{\"namespace\": \"${OPENSHIFT_PROJECT}\", \"admin_user\": \"${OPENSHIFT_USER}\"}"

    wait_until_enmasse_up 'openshift' ${OPENSHIFT_PROJECT} ${UPGRADE}
}

function export_required_env {
    SANITIZED_PROJECT=${OPENSHIFT_PROJECT}
    SANITIZED_PROJECT=${SANITIZED_PROJECT//_/-}
    SANITIZED_PROJECT=${SANITIZED_PROJECT//\//-}
    export OPENSHIFT_PROJECT=$SANITIZED_PROJECT

    export OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
    export OPENSHIFT_USER=${OPENSHIFT_USER:-test}
    export OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
    export OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
    export OPENSHIFT_TEST_LOGDIR=${OPENSHIFT_TEST_LOGDIR:-/tmp/testlogs}
    export OPENSHIFT_USE_TLS=${OPENSHIFT_USE_TLS:-true}
    export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
    export CURDIR=`readlink -f \`dirname $0\``
    export DEFAULT_AUTHSERVICE=standard
}

function wait_until_enmasse_up() {
    CLUSTER_TYPE=${1:-openshift}
    NAMESPACE=${2:-OPENSHIFT_PROJECT}
    UPGRADE=${3:-false}

    expected_pods=6
    if [ "$CLUSTER_TYPE" == "kubernetes" ]; then
        expected_pods=5
    fi

    wait_until_up ${expected_pods} ${NAMESPACE} ${UPGRADE}
    wait_code=$?
    if [ $wait_code -ne 0 ]; then
        err_and_exit 1
    fi

}
function wait_until_up() {
    POD_COUNT=$1
    ADDR_SPACE=$2
    UPGRADED=$3
    ${CURDIR}/wait_until_up.sh ${POD_COUNT} ${ADDR_SPACE} ${UPGRADED} || return 1
}

function wait_until_cluster_up() {
    local timeout=${1}

    NOW=$(date +%s)
    END=$(($NOW + timeout))
    info "Now: $(date -d@${NOW} -u +%F:%H:%M:%S)"
    info "Waiting ${timeout} seconds until: $(date -d@${END} -u +%F:%H:%M:%S)"

    oc cluster status
    while [ $? -gt 0 ]
    do
        NOW=$(date +%s)
        if [ ${NOW} -gt ${END} ]; then
            err_and_exit "ERROR: Timed out waiting for openshift cluster to come up!"
        fi
        sleep 5
        oc cluster status
    done
}

function run_test() {
    TESTCASE=$1
    PROFILE=${2:-systemtests}

    if [ -n "${TESTCASE}" ]; then
        EXTRA_TEST_ARGS="-Dtest=${TESTCASE}"
    fi
    mvn -B test -pl systemtests -P${PROFILE} -Djava.net.preferIPv4Stack=true -DfailIfNoTests=false -Djansi.force=true -Dstyle.color=always ${EXTRA_TEST_ARGS}
}

function teardown_test() {
    PROJECT_NAME=$1
    kubectl delete namespace $PROJECT_NAME
}

function create_address_space() {
    NAMESPACE=$1
    ADDRESS_SPACE_NAME=$2
    ADDRESS_SPACE_DEF=$3
    TOKEN=$(oc whoami -t)
    if [[ ${REGISTER_API_SERVER} == "true" ]]; then
        URL="${OPENSHIFT_URL}"
    else
        URL="https://$(oc get route -o jsonpath='{.spec.host}' restapi)"
    fi
    curl -k -X POST -H "content-type: application/json" --data-binary @${ADDRESS_SPACE_DEF} -H "Authorization: Bearer ${TOKEN}" ${URL}/apis/enmasse.io/v1alpha1/namespaces/${NAMESPACE}/addressspaces
    wait_until_up 2 ${NAMESPACE}-${ADDRESS_SPACE_NAME} || return 1
}

function create_address() {
    NAMESPACE=$1
    ADDRESS_SPACE=$2
    NAME=$3
    ADDRESS=$4
    TYPE=$5
    PLAN=$6

    if [[ ${REGISTER_API_SERVER} == "true" ]]; then
        URL="${OPENSHIFT_URL}"
    else
        URL="https://$(oc get route -o jsonpath='{.spec.host}' restapi)"
    fi

    PAYLOAD="{\"apiVersion\": \"enmasse.io/v1alpha1\", \"kind\": \"AddressList\", \"metadata\": { \"name\": \"${ADDRESS_SPACE}.${NAME}\"}, \"spec\": {\"address\": \"${ADDRESS}\", \"type\": \"${TYPE}\", \"plan\": \"${PLAN}\"}}"
    TOKEN=$(oc whoami -t)
    curl -k -X POST -H "content-type: application/json" -d "${PAYLOAD}" -H "Authorization: Bearer ${TOKEN}" ${URL}/apis/enmasse.io/v1alpha1/namespaces/${NAMESPACE}/addresses
}

function create_user() {
    CLI_ID=$1
    USER=$2
    PASSWORD=$3
    ADDRESS_SPACE_NAME=$4
    NEW_USER_DEF=$5

    info "create new user via user:password ${USER}:${PASSWORD}; ADDRESS_SPACE: '${ADDRESS_SPACE_NAME}'"
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

    info "user: '${USER_NAME}' join group '${GROUP_NAME}'"
    # get token
    RESULT=$(curl -k --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
    TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

    #GET USER ID
    info "get user id: ${USER_NAME}"
    TCKUSERJSON=$(curl -k -X GET -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users?search=${USER_NAME})
    TCK_USER_ID=$(echo ${TCKUSERJSON} | jq -r '.[].id')
    info "user id: ${TCK_USER_ID}"

    #GET GROUP ID
    info "get group id: ${GROUP_NAME}"
    KEYCLOAK_GROUPS=$(curl -k -X GET -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/groups?search=${GROUP_NAME})
    GROUP_ID=$(echo ${KEYCLOAK_GROUPS} | jq '.[]' | jq -r "select(.name == \"${GROUP_NAME}\") | .id")
    info "group id: ${GROUP_ID}"

    #JOIN GROUP
    $(curl -k -X PUT -H "Authorization: Bearer ${TOKEN}"  https://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/${ADDRESS_SPACE_NAME}/users/${TCK_USER_ID}/groups/${GROUP_ID})
}

function create_group() {
    CLI_ID=$1
    USER=$2
    PASSWORD=$3
    ADDRESS_SPACE_NAME=$4
    GROUP_NAME=$5

    info "create new group: '${GROUP_NAME}'"

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

function replace_docker_log_driver() {
    local docker="${1}"
    local docker_config_path="/etc/${docker}/daemon.json"
    local tmpf="$(mktemp)"

    if [[ "$(jq '."log-driver" == "json-file"' "${docker_config_path}")" == "true" ]]; then
        info "docker config already contains log-driver set to json-file"
        return
    fi

    info "stop docker..."
    sudo systemctl stop "${docker}"
    info "create or replace log-driver=json-file in ${docker_config_path}"
    jq '."log-driver"="json-file"' "${docker_config_path}" >"${tmpf}"
    sudo cat "${tmpf}" | sudo tee "${docker_config_path}"
    rm -f "${tmpf}"
}

function get_docker_info() {
    ARTIFACTS_DIR=${1}
    CONTAINER=${2}

    FILENAME_STDOUT="docker_${CONTAINER}.stdout.log"
    FILENAME_STDERR="docker_${CONTAINER}.stderr.log"
    docker logs ${CONTAINER} > ${FILENAME_STDOUT} 2> ${FILENAME_STDERR}
}

function categorize_docker_logs {
    LOG_DIR=${1}
    if [[ "$(ls -A ${LOG_DIR})" ]]; then
        for x in ${LOG_DIR}/*.log; do
            ADDR_SPACE="$(echo "${x##*/}" | sed -e 's/^[^_]\+_\([^_]\+\)_[^_]\+\.log$/\1/')";
            if [[ ! -d "${LOG_DIR}/${ADDR_SPACE}" ]]; then
                mkdir "${LOG_DIR}/${ADDR_SPACE}";
            fi
            mv "${x}" "${LOG_DIR}/${ADDR_SPACE}"
        done
    else
        warn "Directory \"${LOG_DIR}\" with docker logs is empty:"
        ls -la "${LOG_DIR}"
    fi
}

function stop_and_check_openshift() {
    if ! oc; then
        info "oc command not found, nothing to stop"
        return
    fi
    if oc whoami; then
        oc logout
    fi
    oc cluster down #for the case that cluster is already running
    openshift_pids=$(ps aux | grep 'openshift' | grep -v 'grep\|setup-openshift' | awk '{print $2}')
    if [[ -n ${openshift_pids} ]]; then
        warn "OpenShift cluster didn't stop properly, trying to kill OpenShift processes..."
        kill -9 "${openshift_pids}"
    fi

    if oc cluster status; then
        err_and_exit "shutting down of openshift cluster failed, tests won't be executed"
    fi
    info "cluster turned off successfully"
}

function clean_docker_images() {
    DOCKER=${DOCKER:-docker}

    containers_run=$(docker ps -q)
    if [[ -n ${containers_run} ]];then
        ${DOCKER} stop $(docker ps -q)
    else
        info "No running containers"
    fi

    containers_all=$(docker ps -a -q)
    if [[ -n ${containers_all} ]];then
        ${DOCKER} rm $(docker ps -a -q) -f
    else
        info "No containers to remove"
    fi

    images=$(docker images -q)
    if [[ -n ${images} ]];then
        ${DOCKER} rmi $(docker images -q) -f
    else
        info "No images to remove"
    fi
}

function clean_oc_location() {
    info "Removing previous openshift data"
    if [[ $(get_openshift_version) == '3.9'* ]]; then
        info "Cleaning OpenShift work directory /var/lib/origin"
        sudo find  /var/lib/origin -type d -exec mountpoint --quiet {} \; -exec umount --types tmpfs {} \;
        sudo rm -rf /var/lib/origin
    else
        info "Cleaning OpenShift work directory openshift.local.clusterup"
        OLD_OC_LOCATIONS=$(sudo find /home/cloud-user/ -type d -name 'openshift.local.clusterup')
        for i in ${OLD_OC_LOCATIONS}; do sudo find "${i}" -type d -exec mountpoint --quiet {} \; -exec umount --types tmpfs {} \; && rm sudo -rf "${i}"; done
    fi
    sudo rm -rf /var/log/containers/*
    sudo rm -rf /var/log/pods/*
}

function check_if_ansible_ready() {
    info "Running: check if ansible is already installed"
    if ! sudo rpm -qa | grep -qw ansible; then
        info "Ansible is not installed, running install command"
        sudo yum -y install --enablerepo=epel ansible
    fi
}

function get_openshift_version() {
    echo $(oc version | sed -nre '/^oc/s/^.*v(([0-9]+\.)*[0-9]+).*$/\1/p')
}

function get_kubeconfig_path() {
    OC_39='/var/lib/origin/openshift.local.config/master/admin.kubeconfig'
    OC_310='./openshift.local.clusterup/kube-apiserver/admin.kubeconfig'

    if [[ $(get_openshift_version) == '3.9'* ]]; then
        echo $OC_39
    else
        echo $OC_310
    fi
}

function get_oc_args() {
    OC_39='--service-catalog'
    OC_310='--enable=*,service-catalog,web-console --insecure-skip-tls-verify=true'

    if [[ $(get_openshift_version) == '3.9'* ]]; then
        echo $OC_39
    else
        echo $OC_310
    fi
}

function is_upgraded() {
    IMAGE=$1
    TAG=${TAG:-"latest"}
    TEMPLATES=$(cat ${CURDIR}/../../templates/build/enmasse-${TAG}/install/templates/* | grep "image")
    DEPLOYMENTS=$(cat ${CURDIR}/../../templates/build/enmasse-${TAG}/install/components/*/*-Deployment* | grep "image")
    if [[ "${TEMPLATES}" == *"${IMAGE}"* ]] || [[ "${DEPLOYMENTS}" == *"${IMAGE}"* ]] ; then
        echo "true"
    else
        echo "false"
    fi
}

function download_enmasse_release() {
    VERSION=${1:-0.23.1}
    wget https://github.com/EnMasseProject/enmasse/releases/download/${VERSION}/enmasse-${VERSION}.tgz
    rm -rf ${CURDIR}/../../templates/build
    mkdir ${CURDIR}/../../templates/build -p
    tar zxvf enmasse-${VERSION}.tgz -C ${CURDIR}/../../templates/build
}

function get_oc_url() {
    OC_VERSION=${OC_VERSION:-"3.11"}

    if [[ ${OC_VERSION} == "3.11" ]]; then
        echo "https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz"
    elif [[ ${OC_VERSION} == "3.10" ]]; then
        echo "https://github.com/openshift/origin/releases/download/v3.10.0/openshift-origin-client-tools-v3.10.0-dd10d17-linux-64bit.tar.gz"
    fi
}

function wait_until_file_close() {
    FOLDER=$1

    TIMEOUT=120
    NOW=$(date +%s)
    END=$(($NOW + $TIMEOUT))
    info "Waiting until ${END}"
    while true
    do
        NOW=$(date +%s)
        if [[ ${NOW} -gt ${END} ]]; then
            err "Timed out waiting for closing files!"
            err_and_exit "Files in folder ${FOLDER} are still opened"
        fi
        if [[ -n "$(lsof +D ${FOLDER})" ]]; then
            lsof +D "${FOLDER}" || true
            info "Files in ${FOLDER} are still opened"
        else
            info "All files in ${FOLDER} are closed"
            return  0
        fi
        sleep 5
    done
}

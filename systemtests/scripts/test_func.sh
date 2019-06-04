#!/usr/bin/env bash

case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac

CURDIR=`${READLINK} -f \`dirname $0\``

source "${CURDIR}/../../scripts/logger.sh"

function download_enmasse() {
    curl -0 https://dl.bintray.com/enmasse/snapshots/enmasse-latest.tgz | tar -zx
    D=`${READLINK} -f enmasse-latest`
    echo ${D}
}

function setup_test_openshift() {
    TEMPLATES_INSTALL_DIR=$1
    KUBEADM=$2
    SKIP_DEPENDENCIES=${3:-false}
    UPGRADE=${4:-false}
    IMAGE_NAMESPACE=${5:-"enmasseci"}

    export_required_env

    info "Deploying enmasse with templates dir: ${TEMPLATES_INSTALL_DIR}, kubeadmin: ${KUBEADM}, skip setup: ${SKIP_DEPENDENCIES}, upgrade: ${UPGRADE}, namespace: ${KUBERNETES_NAMESPACE}, image namespace: ${IMAGE_NAMESPACE}, iot: ${DEPLOY_IOT}"

    rm -rf ${TEST_LOGDIR}
    mkdir -p ${TEST_LOGDIR}

    oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${KUBERNETES_API_URL}
    oc adm --config ${KUBEADM} policy add-cluster-role-to-user cluster-admin ${OPENSHIFT_USER}
    oc policy add-role-to-group system:image-puller system:serviceaccounts:${KUBERNETES_NAMESPACE} --namespace=${IMAGE_NAMESPACE}
    export KUBERNETES_API_TOKEN=`oc whoami -t`

    if [[ "${SKIP_DEPENDENCIES}" == "false" ]]; then
        ansible-playbook ${CURDIR}/../ansible/playbooks/systemtests-dependencies.yml
    fi
    ansible-playbook ${TEMPLATES_INSTALL_DIR}/ansible/playbooks/openshift/deploy_all.yml -i ${CURDIR}/../ansible/inventory/systemtests.inventory --extra-vars "{\"namespace\": \"${KUBERNETES_NAMESPACE}\", \"admin_user\": \"${OPENSHIFT_USER}\", \"enable_iot\": \"${DEPLOY_IOT}\" }"
    wait_until_enmasse_up 'openshift' ${KUBERNETES_NAMESPACE} ${UPGRADE}
}

function login_user() {
    SET_CLUSTER_USER=${1-true}

    export_required_env

    if [[ "${SET_CLUSTER_USER}" == "true" ]]; then
        oc login system:admin --insecure-skip-tls-verify=true ${KUBERNETES_API_URL} --config $(get_kubeconfig_path)
        oc adm policy add-cluster-role-to-user cluster-admin ${OPENSHIFT_USER} --rolebinding-name=cluster-admin --config $(get_kubeconfig_path)
    fi
    oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true
    export KUBERNETES_API_TOKEN=`oc whoami -t`
}

function export_required_env {
    SANITIZED_NAMESPACE=${OPENSHIFT_PROJECT}
    SANITIZED_NAMESPACE=${SANITIZED_NAMESPACE//_/-}
    SANITIZED_NAMESPACE=${SANITIZED_NAMESPACE//\//-}
    KUBERNETES_NAMESPACE=${SANITIZED_NAMESPACE}

    if oc config view -o jsonpath='{.clusters[0].cluster.server}' > /dev/null 2>&1; then
        API_URL=$(oc config view -o jsonpath='{.clusters[0].cluster.server}')
    fi
    export KUBERNETES_API_URL=${KUBERNETES_API_URL:-${API_URL}}
    export OPENSHIFT_USER=${OPENSHIFT_USER:-test}
    export OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
    export KUBERNETES_NAMESPACE=${KUBERNETES_NAMESPACE:-enmasseci}
    export TEST_LOGDIR=${TEST_LOGDIR:-/tmp/testlogs}
    export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
    export CURDIR=`${READLINK} -f \`dirname $0\``
    export DEFAULT_AUTHSERVICE=standard
}

function wait_until_enmasse_up() {
    CLUSTER_TYPE=${1:-openshift}
    NAMESPACE=${2:-KUBERNETES_NAMESPACE}
    UPGRADE=${3:-false}

    expected_pods=7
    if [[ "${DEPLOY_IOT}" == "true" ]]; then
        expected_pods=$(($expected_pods + 1))
    fi
    if [[ "${CLUSTER_TYPE}" == "kubernetes" ]]; then
        # console won't be deployed on kubernettes by default
        expected_pods=$(($expected_pods - 2))
    fi

    wait_until_up ${expected_pods} ${NAMESPACE} ${UPGRADE}
    wait_code=$?
    if [[ ${wait_code} -ne 0 ]]; then
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
    while [[ $? -gt 0 ]]
    do
        NOW=$(date +%s)
        if [[ ${NOW} -gt ${END} ]]; then
            err_and_exit "ERROR: Timed out waiting for openshift cluster to come up!"
        fi
        sleep 5
        oc cluster status
    done
}

function run_test() {
    PROFILE=${1:-systemtests}
    TESTCASE=${2}

    if [[ -n "${TESTCASE}" ]]; then
        EXTRA_TEST_ARGS="-Dtest=${TESTCASE}"
    fi
    mvn -B test -pl systemtests -am -P${PROFILE} -Djava.net.preferIPv4Stack=true -DfailIfNoTests=false -Djansi.force=true -Dstyle.color=always -DskipTests ${EXTRA_TEST_ARGS}
}

function teardown_test() {
    PROJECT_NAME=$1
    CMD=${2:-oc}
    if [[ ${DEBUG} == 'false' ]]; then
        if [[ ${CMD} == "oc" ]]; then
            ansible-playbook -i ${CURDIR}/../ansible/inventory/systemtests.inventory ${CURDIR}/../../ansible/playbooks/openshift/uninstall.yml --extra-vars "{\"namespace\": \"${KUBERNETES_NAMESPACE}\"}"
        fi
        ${CMD} delete namespace ${PROJECT_NAME}
    fi
    info "End of teardown"
}

function create_address_space() {
    NAMESPACE=$1
    ADDRESS_SPACE_NAME=$2
    TYPE=$3
    PLAN=$4
    AUTH_SERVICE=$5
    API_VERSION=$6
    if [[ "${API_VERSION}" == "v1alpha1" ]]; then
        cat <<EOF | oc create -f -
apiVersion: enmasse.io/${API_VERSION}
kind: AddressSpace
metadata:
    name: ${ADDRESS_SPACE_NAME}
    namespace: ${NAMESPACE}
spec:
    type: ${TYPE}
    plan: ${PLAN}
authenticationService:
    type: standard
EOF
    else
        cat <<EOF | oc create -f -
apiVersion: enmasse.io/${API_VERSION}
kind: AddressSpace
metadata:
    name: ${ADDRESS_SPACE_NAME}
    namespace: ${NAMESPACE}
spec:
    type: ${TYPE}
    plan: ${PLAN}
    authenticationService:
        name: ${AUTH_SERVICE}
EOF
    fi
}

function create_address() {
    NAMESPACE=$1
    ADDRESS_SPACE=$2
    NAME=$3
    ADDRESS=$4
    TYPE=$5
    PLAN=$6
    API_VERSION=$7

    cat <<EOF | oc create -f -
apiVersion: enmasse.io/${API_VERSION}
kind: Address
metadata:
    name: $(echo ${ADDRESS_SPACE}.${NAME//_})
    namespace: ${NAMESPACE}
spec:
    address: ${ADDRESS}
    type: ${TYPE}
    plan: ${PLAN}
EOF
}

function create_user() {
    USER=$1
    PASSWORD=$2
    NAMESPACE=$3
    ADDRESS_SPACE_NAME=$4
    API_VERSION=$5

    cat <<EOF | oc create -f -
apiVersion: user.enmasse.io/${API_VERSION}
kind: MessagingUser
metadata:
    name: $(echo ${ADDRESS_SPACE_NAME}.${USER//_})
    namespace: ${NAMESPACE}
spec:
    username: ${USER}
    authentication:
      type: password
      password: $(echo -n ${PASSWORD} | base64)
    authorization:
    - addresses: [ "*" ]
      operations: [ "send", "recv", "view" ]
    - operations: [ "manage" ]
EOF
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

function print_images() {
    DOCKER=${DOCKER:-docker}
    ${DOCKER} images
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
    OC_39="--public-hostname=$(hostname -I | awk '{print $1}') --service-catalog"
    OC_310="--public-hostname=$(hostname -I | awk '{print $1}') --enable=*,service-catalog,web-console --insecure-skip-tls-verify=true"

    if [[ $(get_openshift_version) == '3.9'* ]]; then
        echo $OC_39
    else
        echo $OC_310
    fi
}

function is_upgraded() {
    IMAGE=${1}
    TAG=${TAG:-"latest"}

    IMAGE_TAG=${IMAGE##*:}
    TEMPLATES=$(cat ${CURDIR}/../../imageenv.txt)
    if [[ "${TEMPLATES}" == *"${IMAGE}"* ]]; then
        echo "true"
    else
        echo "false"
    fi
}

function download_enmasse_release() {
    VERSION=${1:-0.23.1}
    wget https://github.com/EnMasseProject/enmasse/releases/download/${VERSION}/enmasse-${VERSION}.tgz
    rm -rf ${CURDIR}/../../templates/build/enmasse-${VERSION}
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
            warn "Timed out waiting for closing files!"
            kill -9 $(lsof -t +D ${FOLDER}) || true
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

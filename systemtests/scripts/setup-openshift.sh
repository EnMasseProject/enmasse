#!/bin/bash

CURDIR="$(dirname $(readlink -f ${0}))"
source "${CURDIR}/../../scripts/logger.sh"
source "${CURDIR}/test_func.sh"

#parameters:
# {1} path to folder with installation scripts, roles,... (usually templates/install)
SYSTEMTESTS_DIR=${1}
DOCKER=${DOCKER:-docker}


stop_and_check_openshift
check_if_ansible_ready

ansible-playbook ${SYSTEMTESTS_DIR}/ansible/playbooks/environment.yml \
    --extra-vars "{\"openshift_client_url\": \"$(get_oc_url)\"}" -t openshift,kubectl

clean_docker_images
clean_oc_location

DOCKER_STATUS=$(sudo systemctl show --property ActiveState ${DOCKER} | sed -n -e 's/^ActiveState=//p')
if [[ "${DOCKER_STATUS}" != "active" ]]; then
    info "Docker service is not running"
    info "Starting docker service"
    sudo systemctl restart ${DOCKER}
    sudo chmod 777 /var/run/${DOCKER}.sock
fi

OC_CLUSTER_ARGS=$(get_oc_args)
if ! oc cluster up ${OC_CLUSTER_ARGS} ; then
    warn "OpenShift cluster didn't start properly, wait for 30s and try to restart..."
    sleep 30
    oc cluster down
    oc cluster up ${OC_CLUSTER_ARGS}
fi
oc login -u system:admin

TIMEOUT=300
wait_until_cluster_up "${TIMEOUT}"

sleep 30
oc get pv
oc adm policy add-scc-to-group hostmount-anyuid system:serviceaccounts

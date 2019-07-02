#!/bin/bash

CURDIR="$(dirname $(readlink -f ${0}))"
source "${CURDIR}/../../scripts/logger.sh"
source "${CURDIR}/test_func.sh"

SYSTEMTESTS_DIR=${1}

check_if_ansible_ready

ansible-playbook ${SYSTEMTESTS_DIR}/ansible/playbooks/environment.yml \
    --extra-vars "{\"openshift_client_url\": \"$(get_oc_url)\"}" -t openshift
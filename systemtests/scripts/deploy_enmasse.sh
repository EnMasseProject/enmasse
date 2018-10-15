#!/usr/bin/env bash
source ./systemtests/scripts/test_func.sh
SKIP_SETUP=${1:-false}
ENMASSE_DIR=${2}
REG_API_SERVER=${3:-true}
SKIP_DEPENDENCIES=${4:-false}

download_enmasse

if [[ ${SKIP_SETUP} != 'true' ]]; then
    setup_test "${ENMASSE_DIR}" $(get_kubeconfig_path) "${REG_API_SERVER}" "${SKIP_DEPENDENCIES}"
fi

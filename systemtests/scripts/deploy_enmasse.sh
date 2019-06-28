#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

SKIP_SETUP=${1:-false}
ENMASSE_DIR=${2}


if [[ ${SKIP_SETUP} != 'true' ]]; then
    setup_test_openshift "${ENMASSE_DIR}" $(get_kubeconfig_path)
fi

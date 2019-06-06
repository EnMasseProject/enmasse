#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

SET_CLUSTER_USER=${1}

login_user ${SET_CLUSTER_USER}

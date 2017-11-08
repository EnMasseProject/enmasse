#!/usr/bin/env bash
source ./systemtests/scripts/test_func.sh
ENMASSE_DIR=$1
KUBEADM=$2

download_enmasse

setup_test ${ENMASSE_DIR} ${KUBEADM}


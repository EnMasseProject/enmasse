#!/bin/bash
CURDIR="$(dirname $(readlink -f ${0}))"
source "${CURDIR}/test_func.sh"

stop_and_check_openshift
clean_oc_location

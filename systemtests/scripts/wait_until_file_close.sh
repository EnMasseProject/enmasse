#!/usr/bin/env bash
FOLDER=$1
CURDIR="$(readlink -f $(dirname $0))"
source ${CURDIR}/test_func.sh
source "${CURDIR}/../../scripts/logger.sh"

wait_until_file_close ${FOLDER}

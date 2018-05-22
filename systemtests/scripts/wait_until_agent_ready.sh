#!/usr/bin/env bash
EXPECTED_FILE="${1}"
TIMEOUT="${2:-600}"

curdir="$(dirname $(readlink -f ${0}))"

source "${curdir}/../../scripts/logger.sh"

if [[ -z "${EXPECTED_FILE}" ]]; then
    err_and_exit "Argument missing, file name required!" 2
fi

END=$(( SECONDS + TIMEOUT ))
info "Waiting until ${EXPECTED_FILE} will be presented within ${TIMEOUT} seconds timeout!"
while [[ ! -f "${EXPECTED_FILE}" ]]
do
    if (( SECONDS >= END )); then
        err_and_exit "Timeout reached!"
    fi
    info "${EXPECTED_FILE} doesn't exist yet!"
    sleep 5
done
info "All dependencies successfully installed"

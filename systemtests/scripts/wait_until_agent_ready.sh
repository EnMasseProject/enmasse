#!/bin/bash
EXPECTED_FILE="${1}"
TIMEOUT="${2:-600}"

function err() {
    echo "ERROR: ${1}" >&2
    exit ${2:-1}
}

function info() {
    echo "INFO: ${1}"
}

if [[ -z "${EXPECTED_FILE}" ]]; then
    err "Argument missing, file name required!" 2
fi

END=$(( SECONDS + TIMEOUT ))
info "Waiting until ${EXPECTED_FILE} will be presented within ${TIMEOUT} seconds timeout!"
while [[ ! -f "${EXPECTED_FILE}" ]]
do
    if (( SECONDS >= END )); then
        err "Timeout reached!"
    fi
    info "${EXPECTED_FILE} doesn't exist yet!"
    sleep 5
done
info "All dependencies successfully installed"

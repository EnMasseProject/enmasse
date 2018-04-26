#!/usr/bin/env bash
CORE_DUMPS_LOCATION=${1}

if [[ $(ls -A "${CORE_DUMPS_LOCATION}") ]]; then
    echo "FAIL: Core dumps found!"
    sudo rm -rf ${CORE_DUMPS_LOCATION}
    exit 1
else
    echo "No core dumps found!"
fi

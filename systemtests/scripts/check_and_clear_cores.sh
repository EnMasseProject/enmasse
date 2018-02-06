#!/usr/bin/env bash
CORE_DUMPS_LOCATION=${1}

if [ ! -z "$(ls -A ${CORE_DUMPS_LOCATION})" ]; then
    sudo rm -rf ${CORE_DUMPS_LOCATION}
    exit 1
fi

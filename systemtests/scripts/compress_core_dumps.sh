#!/usr/bin/env bash
CORE_DUMPS_LOCATION=${1}
ARTIFACTS_DIR=${2}
if [[ $(ls -A "${CORE_DUMPS_LOCATION}") ]]; then
    while [[ -n "$(lsof +D "${CORE_DUMPS_LOCATION}")" ]] ; do
        echo "Core dumps folder is not ready to compress!"
        lsof +D "${CORE_DUMPS_LOCATION}"
        sleep 5
    done
    echo "Core dumps folder is ready to compress!"
    sudo tar -czvf core-dumps.tar.gz ${CORE_DUMPS_LOCATION}
    sudo mv core-dumps.tar.gz ${ARTIFACTS_DIR}
else
    echo "Core dump folder is empty"
fi

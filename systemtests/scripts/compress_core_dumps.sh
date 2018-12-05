#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

CORE_DUMPS_LOCATION=${1}
ARTIFACTS_DIR=${2}
if [[ $(ls -A "${CORE_DUMPS_LOCATION}") ]]; then
    wait_until_file_close ${CORE_DUMPS_LOCATION}
    echo "Core dumps folder is ready to compress!"
    sudo tar -czvf core-dumps.tar.gz ${CORE_DUMPS_LOCATION}
    sudo mv core-dumps.tar.gz ${ARTIFACTS_DIR}
else
    echo "Core dump folder is empty"
fi

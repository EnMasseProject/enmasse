#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

CLI_ID=$1
DEFAULT_ADDRESS_SPACE=$2

#install prerequisites
sudo yum -y install patch

#setup environment
create_addres_space ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckAddressSpace.json' || return 1
create_addresses ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckAddresses.json'
create_user ${CLI_ID} './systemtests/templates/tckUser.json' ${DEFAULT_ADDRESS_SPACE}

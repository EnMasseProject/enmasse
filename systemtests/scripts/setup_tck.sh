#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

CLI_ID=$1
DEFAULT_ADDRESS_SPACE=$2

#install prerequisites
sudo yum -y install patch jq

oc extract secret/keycloak-credentials --confirm
USER=$(cat admin.username)
PASSWORD=$(cat admin.password)

#setup environment
create_addres_space ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckAddressSpace.json' || return 1
create_addresses ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckAddresses.json'

#setup user and groups
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "send_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "recv_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "manage_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "view_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "monitor"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "admin"
create_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "manage"

create_user ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckUser.json'

TCK_USER=$(cat ./systemtests/templates/tckUser.json | jq -r '.username')

join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "manage"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "admin"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "monitor"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "view_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "manage_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "recv_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} ${DEFAULT_ADDRESS_SPACE} "${TCK_USER}" "send_#"

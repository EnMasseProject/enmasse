#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

NAMESPACE=$1
CLI_ID=$2
DEFAULT_ADDRESS_SPACE=$3

#install prerequisites
sudo yum -y install patch jq

oc extract secret/keycloak-credentials --confirm
USER=$(cat admin.username)
PASSWORD=$(cat admin.password)

#setup environment
create_address_space ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} './systemtests/templates/tckAddressSpace.json' || return 1

create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "MY_QUEUE_upper" "MY_QUEUE" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "MY_QUEUE2" "MY_QUEUE2" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testQ0" "testQ0" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testQ1" "testQ1" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testQ2" "testQ2" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testQueue2" "testQueue2" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "Q2" "Q2" "queue" "brokered-queue"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "myQueue" "myQueue" "queue" "brokered-queue"

create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "MY_TOPIC_upper" "MY_TOPIC" "topic" "brokered-topic"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "MY_TOPIC2" "MY_TOPIC2" "topic" "brokered-topic"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testT0" "testT0" "topic" "brokered-topic"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testT1" "testT1" "topic" "brokered-topic"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "testT2" "testT2" "topic" "brokered-topic"
create_address ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} "myTopic" "myTopic" "topic" "brokered-topic"

sleep 60 #waiting for addresses are ready

#setup user and groups
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "send_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "recv_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "manage_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "view_#"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "monitor"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "admin"
create_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "manage"

create_user ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" './systemtests/templates/tckUser.json'

TCK_USER=$(cat ./systemtests/templates/tckUser.json | jq -r '.username')

join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "manage"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "admin"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "monitor"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "view_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "manage_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "recv_#"
join_group ${CLI_ID} ${USER} ${PASSWORD} "${NAMESPACE}-${DEFAULT_ADDRESS_SPACE}" "${TCK_USER}" "send_#"

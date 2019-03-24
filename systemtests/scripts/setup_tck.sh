#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

NAMESPACE=$1
DEFAULT_ADDRESS_SPACE=$2

#setup environment
create_address_space ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE} || return 1

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

create_user "tckuser" "tckuser" ${NAMESPACE} ${DEFAULT_ADDRESS_SPACE}

sleep 120

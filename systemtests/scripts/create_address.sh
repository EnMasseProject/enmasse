#!/usr/bin/env bash

case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac

CURDIR=`${READLINK} -f \`dirname $0\``
source ${CURDIR}/test_func.sh

NAMESPACE=$1
ADDRESS_SPACE=$2
NAME=$3
ADDRESS=$4
TYPE=$5
PLAN=$6
API_VERSION=${7:-v1beta1}


create_address ${NAMESPACE} ${ADDRESS_SPACE} ${NAME} ${ADDRESS} ${TYPE} ${PLAN} ${API_VERSION}

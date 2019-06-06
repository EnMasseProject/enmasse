#!/usr/bin/env bash

case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac

CURDIR=`${READLINK} -f \`dirname $0\``
source ${CURDIR}/test_func.sh

NAMESPACE=$1
ADDRESS_SPACE_NAME=$2
TYPE=$3
PLAN=$4
AUTH_SERVICE=$5
API_VERSION=${6:-v1beta1}


create_address_space ${NAMESPACE} ${ADDRESS_SPACE_NAME} ${TYPE} ${PLAN} ${AUTH_SERVICE} ${API_VERSION}

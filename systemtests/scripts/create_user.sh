#!/usr/bin/env bash
case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac

CURDIR=`${READLINK} -f \`dirname $0\``
source ${CURDIR}/test_func.sh

USER=$1
PASSWORD=$2
NAMESPACE=$3
ADDRESS_SPACE_NAME=$4
API_VERSION=${6:-v1beta1}


create_user ${USER} ${PASSWORD} ${NAMESPACE} ${ADDRESS_SPACE_NAME} ${API_VERSION}

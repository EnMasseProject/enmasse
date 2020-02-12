#!/usr/bin/env bash
case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac

CURDIR=`${READLINK} -f \`dirname $0\``
source ${CURDIR}/test_func.sh

    NAMESPACE=$1
    NAME=$2
    API_VERSION=$3

create_iot_congif ${NAMESPACE} ${NAME} ${API_VERSION}

#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

VERSION=${1:-0.23.0}

download_enmasse_release ${VERSION}

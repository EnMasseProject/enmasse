#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

export TAG=${TAG:-latest}
export DOCKER_ORG=${DOCKER_ORG:-$USER}
export DOCKER_REGISTRY="localhost:5000"

# We do a slim build and drop all output to shrink the console output

make SKIP_TESTS=true > /dev/null
make docker_build > /dev/null
make -j 4 docker_tag docker_push > /dev/null
make -C templates > /dev/null

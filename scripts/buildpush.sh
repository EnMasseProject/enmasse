#!/bin/bash
MODULE=${1}
DOCKER_ORG=${DOCKER_ORG:-$USER}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-docker.io}

export DOCKER_ORG
export DOCKER_REGISTRY

echo "Using docker registry ${DOCKER_REGISTRY}"
echo "Using docker org ${DOCKER_ORG}"

if [ "$MODULE" != "" ]; then
    echo "Restricting build to ${MODULE}"
    pushd $MODULE
fi
make && make docker_tag && make docker_push
if [ "$MODULE" != "" ]; then
    popd
fi

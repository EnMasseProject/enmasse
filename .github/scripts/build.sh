#!/bin/bash
set -e

echo $VERSION

echo "Make"
make

echo "Build"
make docker_build

if [[ -v RELEASE ]]
then
    echo "Logging in to Docker Hub"
    docker login -u ${REGISTRY_USER} -p ${REGISTRY_PASS} ${DOCKER_REGISTRY}
    make TAG=${VERSION} docker_tag docker_push
fi

echo "Push to registry"
make -j 4 docker_tag docker_push

echo "Generate templates"
make templates
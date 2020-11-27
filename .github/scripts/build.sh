#!/bin/bash
set -e

echo "${VERSION}"
export SKIP_TESTS=true
echo "Make"
make

echo "Build"
make docker_build

if [[ -v PUSH_REGISTRY ]]
then
    echo "Logging in to registry"
    docker login -u "${REGISTRY_USER}" -p "${REGISTRY_PASS}" "${DOCKER_REGISTRY}"
    make TAG="${VERSION}" docker_tag docker_push
fi

echo "Push to registry"
make -j 4 docker_tag docker_push

echo "Generate templates"
make templates

echo "Generate OLM index"
make olmindex olmindex_push

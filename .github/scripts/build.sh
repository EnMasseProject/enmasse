#!/bin/bash
set -e

echo "${VERSION}"

if [[ -v PUSH_REGISTRY ]]
then
    echo "Logging in to registry"
    docker login -u "${REGISTRY_USER}" -p "${REGISTRY_PASS}" "${DOCKER_REGISTRY}"
    make TAG="${VERSION}" buildpush
else
    make IMAGE_PULL_POLICY=IfNotPresent buildpushkind
fi

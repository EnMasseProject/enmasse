#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
VERSION=$(grep "release.version" pom.properties| cut -d'=' -f2)
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}

if use_external_registry
then
    export IMAGE_VERSION=${TAG}
else
    export DOCKER_REGISTRY="localhost:5000"
fi

echo "Building EnMasse with tag $TAG, version $VERSION from $BRANCH. PR: $PULL_REQUEST"
make clean

time make

time make docker_build

echo "Tagging Docker Images"
if use_external_registry
then
    make docker_tag
    make TAG=${VERSION} docker_tag
else
    make docker_tag
fi

if use_external_registry
then
    echo "Logging in to Docker Hub"
    docker login -u ${DOCKER_USER} -p ${DOCKER_PASS} ${DOCKER_REGISTRY}
    echo "Pushing images to Docker Hub"
    make docker_push
    make TAG=${VERSION} docker_push
else
    echo "Pushing images to Local Docker Registry"
    make docker_push
fi

#!/bin/bash
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh

PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
VERSION=$(cat release.version)
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}
SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS:-"io.enmasse.**.SmokeTest"}
SYSTEMTEST_PROFILE=${SYSTEMTEST_PROFILE:-"smoke"}

if use_external_registry
then
    export IMAGE_VERSION=${TAG}
else
    export DOCKER_REGISTRY="localhost:5000"
fi

echo "Building EnMasse with tag $TAG, version $VERSION from $BRANCH. PR: $PULL_REQUEST"
make clean

make

make docu_html

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
    docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
    echo "Pushing images to Docker Hub"
    make docker_push
    make TAG=${VERSION} docker_push
else
    echo "Pushing images to Local Docker Registry"
    make docker_push
fi

echo "Running systemtests"
./systemtests/scripts/run_test_kubernetes.sh templates/build/enmasse-${TAG} ${SYSTEMTEST_PROFILE} ${SYSTEMTEST_ARGS}

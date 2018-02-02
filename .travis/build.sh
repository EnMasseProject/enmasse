#!/bin/sh
set -e
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
COMMIT=${COMMIT:-latest}
VERSION=`cat release.version`
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}
SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS:-"io.enmasse.**.SmokeTest"}

if [ "$TAG" != "latest" ]; then
    COMMIT=$TAG
fi

if [ "$BRANCH" != "master" ] && [ "$BRANCH" != "$VERSION" ] || [ "$PULL_REQUEST" != "false" ]
then
    export DOCKER_REGISTRY="localhost:5000"
fi

export MOCHA_ARGS="--reporter=mocha-junit-reporter"

echo "Building EnMasse with tag $TAG, commit $COMMIT, version $VERSION from $BRANCH. PR: $PULL_REQUEST"
make clean

make

echo "Tagging Docker Images"
make TAG=$COMMIT docker_tag
#
if [ "$BRANCH" != "master" ] && [ "$BRANCH" != "$TAG" ] || [ "$PULL_REQUEST" != "false" ]
then
    echo "Using local registry"
else
    make docker_tag
    echo "Logging in to Docker Hub"
    docker login -u $DOCKER_USER -p $DOCKER_PASS
fi

echo "Pushing images to Docker Registry"
make TAG=$COMMIT docker_push

echo "Running systemtests"
./systemtests/scripts/run_test_kubernetes.sh templates/install ${SYSTEMTEST_ARGS}

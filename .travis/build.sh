#!/bin/sh
set -e
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
VERSION=`cat release.version`
TAG=${TAG:-latest}
DOCKER_ORG=enmasseproject
SYSTEMTEST_ARGS=${SYSTEMTEST_ARGS:-"io.enmasse.**.SmokeTest"}
SYSTEMTEST_PROFILE=${SYSTEMTEST_PROFILE:-"smoke"}
export DOCKER_REGISTRY=docker.io

echo "Building EnMasse with tag $TAG, version $VERSION from $BRANCH. PR: $PULL_REQUEST"
make clean

make templates || true

echo "Running systemtests"
./systemtests/scripts/run_test_kubernetes.sh templates/build/enmasse-${TAG} ${SYSTEMTEST_PROFILE} ${SYSTEMTEST_ARGS}

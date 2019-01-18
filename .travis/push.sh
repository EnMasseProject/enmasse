#!/bin/sh
set -e
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
VERSION=`cat release.version`
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}

if [ "$BRANCH" != "master" ] && [ "$BRANCH" != "$TAG" ] || [ "$PULL_REQUEST" != "false" ]
then
    echo "Skipping docker tag on PR"
else
    make docker_push
    make TAG=${VERSION} docker_push
fi

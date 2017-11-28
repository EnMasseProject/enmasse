#!/bin/sh
set -e
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
COMMIT=${COMMIT:-latest}
VERSION=`cat release.version`
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}

if [ "$TAG" != "latest" ]; then
    COMMIT=$TAG
fi

if [ "$BRANCH" != "master" ] && [ "$BRANCH" != "$TAG" ] || [ "$PULL_REQUEST" != "false" ]
then
    echo "Skipping docker tag on PR"
else
    make docker_push
fi

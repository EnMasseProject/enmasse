#!/bin/sh
set -e
VERSION=${VERSION:-latest}
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}

if [ "$VERSION" != "latest" ]; then
    TAG=$VERSION
fi

if [ "$BRANCH" != "master" ] && [ "$BRANCH" != "$VERSION" ] || [ "$PULL_REQUEST" != "false" ]
then
    echo "Skipping docker tag on PR"
else
    make UPLOAD_TAG=$VERSION docker_push
fi

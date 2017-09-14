#!/bin/sh
set -e
export VERSION=${TRAVIS_TAG:-latest}

if [ "$VERSION" != "latest" ]; then
    export TAG=$VERSION
fi

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    make UPLOAD_TAG=latest docker_push
else
    echo "Skipping docker tag on PR"
fi

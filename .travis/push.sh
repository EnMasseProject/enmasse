#!/bin/sh
set -e
export VERSION=${TRAVIS_TAG:-latest}

if [ "$VERSION" != "latest" ]; then
    export TAG=$VERSION
fi

if [ "$TRAVIS_BRANCH" != "master" ] && [ "$TRAVIS_BRANCH" != "$TRAVIS_TAG" ] || [ "$TRAVIS_PULL_REQUEST" != "false" ]
then
    echo "Skipping docker tag on PR"
else
    make UPLOAD_TAG=$VERSION docker_push
fi

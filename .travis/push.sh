#!/bin/sh
set -e
export VERSION=${TRAVIS_TAG:-latest}

if [ "$VERSION" != "latest" ]; then
    export TAG=$VERSION
fi

make TAG=latest docker_tag
make TAG=latest docker_push

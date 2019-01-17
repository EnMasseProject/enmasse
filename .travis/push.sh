#!/bin/sh
set -e
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh
PULL_REQUEST=${PULL_REQUEST:-true}
BRANCH=${BRANCH:-master}
VERSION=`cat release.version`
TAG=${TAG:-latest}
DOCKER_ORG=${DOCKER_ORG:-$USER}

if use_external_registry
then
    make docker_push
    make TAG=${VERSION} docker_push
else
    echo "Skipping docker tag on PR"
fi

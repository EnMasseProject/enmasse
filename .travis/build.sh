#!/bin/sh
set -e
export VERSION=${TRAVIS_TAG:-latest}

if [ "$VERSION" != "latest" ]; then
    export TAG=$VERSION
fi

echo "Building EnMasse"
make

echo "Tagging Docker Images"
make docker_tag

echo "Logging in to Docker Hub"
docker login -u $DOCKER_USER -p $DOCKER_PASS

echo "Pushing images to Docker Hub"
make docker_push

echo "Running systemtests"
./systemtests/scripts/run_test_component.sh templates/install /tmp/openshift systemtests

echo "Generating bintray artifact descriptor"
./.travis/generate-bintray-descriptor.sh enmasse templates/build/enmasse-${VERSION}.tgz > .bintray.json

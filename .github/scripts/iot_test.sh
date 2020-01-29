#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running iot tests"
time make SYSTEMTEST_PROFILE=shared-iot systemtests

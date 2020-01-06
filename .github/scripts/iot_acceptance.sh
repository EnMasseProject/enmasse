#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running iot acceptance tests"
time make SYSTEMTEST_PROFILE=acceptance SYSTEMTEST_ARGS=iot.** systemtests

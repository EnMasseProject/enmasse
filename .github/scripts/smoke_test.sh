#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/default/enmasse-${TAG}

echo "Running smoke tests"
time make PROFILE=smoke systemtests

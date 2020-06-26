#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

export OCP4_EXTERNAL_IMAGE_REGISTRY=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' image-registry):5000

echo "OLM TESTS DISABLED"
# time make TESTCASE=olm.** PROFILE=olm-pr systemtests

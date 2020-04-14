#!/usr/bin/env bash
FROM=${1}
CUSTOM_IMAGE=${2}

DOCKER=${DOCKER:-docker}

rm -rf manifests
id=$($DOCKER create $FROM)
$DOCKER cp $id:/manifests $PWD
echo "manifests copied"
MANIFESTS_CONTENT_REPLACER=./manifests_replacer.sh
if [ -f "$MANIFESTS_CONTENT_REPLACER" ]; then
    echo "executing $MANIFESTS_CONTENT_REPLACER"
    bash $MANIFESTS_CONTENT_REPLACER
fi
$DOCKER rm -v $id

$DOCKER build -t $CUSTOM_IMAGE .

rm -rf manifests

$DOCKER login $OCP4_EXTERNAL_IMAGE_REGISTRY -u $(oc whoami) -p $(oc whoami -t)

$DOCKER push $CUSTOM_IMAGE
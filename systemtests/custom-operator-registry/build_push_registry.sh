#!/usr/bin/env bash
FROM=${1}
CUSTOM_IMAGE=${2}

rm -rf manifests
id=$(docker create $FROM)
docker cp $id:/manifests $PWD
echo "manifests copied"
MANIFESTS_CONTENT_REPLACER=./manifests_replacer.sh
if [ -f "$MANIFESTS_CONTENT_REPLACER" ]; then
    echo "executing $MANIFESTS_CONTENT_REPLACER"
    bash $MANIFESTS_CONTENT_REPLACER
fi
docker rm -v $id

docker build -t $CUSTOM_IMAGE .

rm -rf manifests

docker login $OCP4_EXTERNAL_IMAGE_REGISTRY -u $(oc whoami) -p $(oc whoami -t)

docker push $CUSTOM_IMAGE
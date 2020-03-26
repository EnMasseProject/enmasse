#!/usr/bin/env bash
FROM=${1}
CUSTOM_IMAGE=${2}
REGISTRY=${3}

rm -rf manifests
id=$(docker create $FROM)
docker cp $id:/manifests $PWD
if [ "${REGISTRY}" != "" ]; then
    find manifests -name "*.yaml" | xargs sed -e "s,registry.redhat.io/amq7/amq-online-,${REGISTRY}/rh-osbs/amq7-amq-online-,g" -i
    find manifests -name "*.yaml" | xargs sed -e "s,registry.redhat.io/amq7-tech-preview/amq-online-,${REGISTRY}/rh-osbs/amq7-tech-preview-amq-online-,g" -i
fi
docker rm -v $id

docker build -t $CUSTOM_IMAGE .

rm -rf manifests

docker login $OCP4_EXTERNAL_IMAGE_REGISTRY -u $(oc whoami) -p $(oc whoami -t)

docker push $CUSTOM_IMAGE
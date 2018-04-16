#!/usr/bin/env bash
#parameters
#{1} HOURS - registry older then now-${HOURS} will be removed
#{2} DOCKER_PASS - docker password
#{3} REGISTRY_URL - url of docker registry
HOURS=${1}
DOCKER_PASS=${2}
REGISTRY_URL=${3}

oc login --insecure-skip-tls-verify --token ${DOCKER_PASS} ${REGISTRY_URL}
oc project "enmasseproject"

DATE_FORMAT='+%Y-%m-%dT%H:%M:%SZ'
TARGET_DATE="$(date -d "-${HOURS} hours")" #now - hours
TARGET_DATE="$(date -d "${TARGET_DATE}" "${DATE_FORMAT}")" #format date
echo "All image streams tags older then ${HOURS} hours will be removed! ${TARGET_DATE}"

ITEMS_COUNT="$(oc get imagestreamtags -o json | jq  '.items | length')"
ALL_STREAMS="$(oc get imagestreamtags -o json)"
OLD_IMAGES=""

for (( i=0; i<${ITEMS_COUNT}; i++ ))
do
    img_stream_creation=$(echo "${ALL_STREAMS}" | jq ".items[${i}].metadata.creationTimestamp")
    img_stream_creation="${img_stream_creation//\"/}" #remove quotas from date
    creation_stamp_seconds=$(date --date="${img_stream_creation}" '+%s')
    target_date_seconds=$(date --date="${TARGET_DATE}" '+%s')

    if (( creation_stamp_seconds < target_date_seconds )); then
        img_stream_tag=$(echo  "${ALL_STREAMS}" | jq ".items[${i}].metadata.name")
        echo "Image stream :'${img_stream_tag}' is older then ${HOURS} and will be removed"
        OLD_IMAGES="${OLD_IMAGES} ${img_stream_tag}"
    fi
done

if [[ ! -z ${OLD_IMAGES} ]]; then
    OLD_IMAGES="${OLD_IMAGES//\"/}" #remove quotas from OLD_IMAGES
    echo "Execute: oc delete imagestreamtags ${OLD_IMAGES}"
    oc delete imagestreamtags ${OLD_IMAGES}
else
    echo "There are no images that should be removed!"
fi
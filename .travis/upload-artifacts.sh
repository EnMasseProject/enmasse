#!/bin/bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/common.sh
SUCCESS=${1}
COMMIT=${COMMIT:-latest}
VERSION=`grep "release.version" pom.properties| cut -d'=' -f2`
TAG=${TAG:-latest}

if [[ "${TAG}" != "latest" ]]; then
    COMMIT=${TAG}
fi

export PACKAGE=enmasse
export REPOSITORY="travis"
if [[ "$TAG" != "latest" ]]
then
    export REPOSITORY="releases"
    export TRAVIS_BUILD_NUMBER="."
fi

function upload_file() {
    local file=${1}
    local target=${2}
    if [[ -f ${file} ]]; then
        echo "curl -T $file -u${BINTRAY_API_USER}:${BINTRAY_API_TOKEN} -H 'X-Bintray-Package:${PACKAGE}' -H 'X-Bintray-Version:${TAG}' https://api.bintray.com/content/enmasse/${REPOSITORY}/${target}"
        curl -T ${file} -u${BINTRAY_API_USER}:${BINTRAY_API_TOKEN} -H "X-Bintray-Package:${PACKAGE}" -H "X-Bintray-Version:${TAG}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${REPOSITORY}/${target}
    else
        echo "Skipping ${file}, not found"
    fi
}

function upload_folder() {
    local folder=$1
    local target=$2
    for i in `find ${folder} -type f`
    do
        dest=`echo ${i} | cut -f 2- -d '/'`
        upload_file ${i} "$target/$dest"
    done
}

if [[ "$SUCCESS" == "true" ]]; then
    if use_external_registry
    then
        echo "Uploading snapshot for ${TRAVIS_BRANCH} build"
        upload_file templates/build/enmasse-${TAG}.tgz enmasse-${TAG}.tgz
    else
        echo "Skipping upload for successful PR"
    fi
else
    echo "Collecting test reports"
    
    mkdir -p artifacts/test-reports
    for i in `find . -name "TEST-*.xml"`
    do
        cp ${i} artifacts/test-reports
    done

    cp templates/build/enmasse-${TAG}.tgz artifacts/

    ARTIFACTS=artifacts-${TRAVIS_BUILD_NUMBER}
    mkdir -p ${ARTIFACTS}
    mv artifacts/* ${ARTIFACTS}/
    tar -czf ${ARTIFACTS}.tgz ${ARTIFACTS}
    upload_file ${ARTIFACTS}.tgz ${TRAVIS_BUILD_NUMBER}/${ARTIFACTS}.tgz
fi

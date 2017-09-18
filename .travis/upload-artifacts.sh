#!/bin/bash
SUCCESS=$1
VERSION=${TRAVIS_TAG:-latest}
if [ "$VERSION" != "latest" ]; then
    TAG=$VERSION
fi

export PACKAGE=enmasse
export REPOSITORY="snapshots"
if [ -n "$TRAVIS_TAG" ]
then
    export REPOSITORY="releases"
    export TRAVIS_BUILD_NUMBER="."
fi

function upload_file() {
    local file=$1
    local target=$2
    if [ -f $file ]; then
        echo "curl -T $file -u${BINTRAY_API_USER}:${BINTRAY_API_TOKEN} -H 'X-Bintray-Package:${PACKAGE}' -H 'X-Bintray-Version:${VERSION}' https://api.bintray.com/content/enmasse/snapshots/$target"
        curl -T $file -u${BINTRAY_API_USER}:${BINTRAY_API_TOKEN} -H "X-Bintray-Package:${PACKAGE}" -H "X-Bintray-Version:${VERSION}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/snapshots/$target
    else
        echo "Skipping $file, not found"
    fi
}

function upload_folder() {
    local folder=$1
    local target=$2
    for i in `find $folder -type f`
    do
        dest=`echo $i | cut -f 2- -d '/'`
        upload_file $i "$target/$dest"
    done
}

if [ "$SUCCESS" == "true" ]; then
    if [ "$TRAVIS_BRANCH" != "master" ] || [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
        echo "Skipping upload for successful PR"
    else
        echo "Uploading snapshot for master build"
        upload_file templates/build/enmasse-${VERSION}.tgz enmasse-${VERSION}.tgz
    fi
else
    echo "Collecting test reports"
    
    mkdir -p artifacts/test-reports
    for i in `find . -name "TEST-*.xml"`
    do
        cp $i artifacts/test-reports
    done
    tar -czf artifacts/test-reports.tgz artifacts/test-reports

    upload_file templates/build/enmasse-${VERSION}.tgz $TRAVIS_BUILD_NUMBER/enmasse-${VERSION}.tgz
    upload_folder artifacts $TRAVIS_BUILD_NUMBER/artifacts
fi

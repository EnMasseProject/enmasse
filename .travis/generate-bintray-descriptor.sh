#!/bin/bash

# Assumes that BINTRAY_API_USER and BINTRAY_API_KEY to be set
PACKAGE=$1
shift
FILES=$@

TAG="latest"
REPOSITORY="snapshots"

if [ -n "$TRAVIS_TAG" ]
then
    TAG="$TRAVIS_TAG"
    REPOSITORY="releases"
fi

if [ "$TRAVIS_BRANCH" == "master" ] || [ -n "$TRAVIS_TAG" ]
then
    if [ "$TRAVIS_PULL_REQUEST" == "false" ]
    then
cat<<EOF
{
    "package": {
        "name": "${PACKAGE}",
        "repo": "${REPOSITORY}",
        "subject": "enmasse",
        "desc": "${PACKAGE} built by travis",
        "website_url": "enmasseproject.github.io",
        "issue_tracker_url": "https://github.com/EnMasseProject/${PACKAGE}/issues",
        "vcs_url": "https://github.com/EnMasseProject/${PACKAGE}.git",
        "github_use_tag_release_notes": false,
        "licenses": ["Apache-2.0"],
        "public_download_numbers": true,
        "public_stats": true 
    },

    "version": {
        "name": "${TAG}"
    },

    "files": [
EOF
    n=0
    COMMA=""
    for file in $FILES
    do
        base=`basename $file`
        if [ $n -gt 0 ]; then
            COMMA=","
        fi
        echo "       $COMMA{\"includePattern\": \"$file\", \"uploadPattern\": \"${TAG}/$base\", \"matrixParams\": {\"override\": 1}}"
        n=$(($n + 1))
    done
cat<<EOF
    ],
    "publish": true
}
EOF
    fi
fi


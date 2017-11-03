#!/bin/bash
REPO=${1:-"enmasse.io"}

if [ "$TRAVIS_BRANCH" == "master" ]
then
    body="{\"request\": { \"message\": \"Triggered by EnMasse build\",\"branch\":\"master\" }}"

    curl -s -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Travis-API-Version: 3" \
      -H "Authorization: token $TRAVIS_TOKEN" \
      -d "$body" \
      https://api.travis-ci.org/repo/EnMasseProject%2F${REPO}/requests
else
    echo "Not triggering docs build for $TRAVIS_BRANCH"
fi

#!/bin/sh

dir=`git rev-parse --show-toplevel`
orgDir=`dirname $dir`

org=`basename $orgDir`
repo=`basename $dir`

reponame="$org/$repo"

body="{\"request\": { \"message\": \"Triggered by $reponame\",\"branch\":\"master\" }}"

curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token $TRAVIS_TOKEN" \
  -d "$body" \
  https://api.travis-ci.org/repo/EnMasseProject%2Fsmoketest/requests

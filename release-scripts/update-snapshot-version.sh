#!/bin/sh
VERSION=$1

git checkout master

mvn versions:set -DnewVersion=${VERSION}

git commit -a -m "Update pom.xml snapshot versions to ${VERSION}"

echo "Press ENTER to push pom changes to GitHub"
read

git push origin master --tags

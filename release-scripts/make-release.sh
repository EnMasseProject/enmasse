#!/bin/sh
VERSION=$1
git checkout -b release-${VERSION}
mvn versions:set -DnewVersion=${VERSION}
git commit -a -m "Update version to ${VERSION} in pom files"
git tag ${VERSION}
echo "Press ENTER to push the ${VERSION} branch and tag to GitHub"
read
git push -u origin release-${VERSION}

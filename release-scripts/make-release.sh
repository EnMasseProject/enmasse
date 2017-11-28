#!/bin/sh
VERSION=$1
git checkout -b release-${VERSION}
mvn versions:set -DnewVersion=${VERSION}
echo $VERSION > release.version
git status
echo "Press ENTER to commit ${VERSION}"
read
git commit -a -m "Update version to ${VERSION}"
git tag ${VERSION}
echo "Press ENTER to push the ${VERSION} branch and tag to GitHub"
read
git push -u origin release-${VERSION}
git push -u origin ${VERSION}

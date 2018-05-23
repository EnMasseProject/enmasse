#!/bin/sh
RELEASE_BRANCH=$1
VERSION=$2
git checkout release-${RELEASE_BRANCH}
mvn versions:set -DnewVersion=${VERSION}
echo $VERSION > release.version
git status
echo "Press ENTER to commit ${VERSION}"
read
git commit -a -m "Update version to ${VERSION}"
git tag ${VERSION}
echo "Press ENTER to push the ${VERSION} branch and tag to GitHub"
read
git push -u origin release-${RELEASE_BRANCH}
git push -u origin ${VERSION}

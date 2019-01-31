#!/bin/sh
VERSION=$1
git checkout -b release-${VERSION}
mvn versions:set -DnewVersion=${VERSION}
sed -i "/release\.version=/ s/=.*/=${VERSION}/" pom.properties
sed -i "/maven\.version=/ s/=.*/=${VERSION}/" pom.properties
git status
echo "Press ENTER to commit ${VERSION}"
read
git commit -a -m "Update version to ${VERSION}"
echo "Press ENTER to push the ${VERSION} branch to GitHub"
read
git push -u origin release-${VERSION}

#!/bin/sh
RELEASE_BRANCH=$1
git checkout release-${RELEASE_BRANCH}
git fetch origin master
echo "Press ENTER to force push the rebased ${VERSION} branch to GitHub"
read
git rebase origin/master && git push -f origin release-${RELEASE_BRANCH}

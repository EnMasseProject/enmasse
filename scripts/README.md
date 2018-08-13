# Scripts for creating EnMasse releases

This repository contains scripts for branching and tagging releases of EnMasse. EnMasse release
branches are created for every minor version of EnMasse, and each release within that minor version
is created by tagging and pushing the tag to github.

## Creating a new release branch

The `branch-release.sh` script creates a release branch for a minor version. To create a new release
branch, simply run it with the version name as argument (NB: important to run it from the repository
root folder):

```
./scripts/branch-release.sh 0.22
```

The script will go through the following steps, pausing after each to allow for cancelling the
operation.

1. Create local branch and update versions
2. Commit changes to local git repository
3. Push changes to the upstream repository

## Rebasing a release branch

When creating micro-releases (i.e. 0.21.X), the `rebase-release.sh` script should be used to rebase
the release branch before tagging the new release (NB: important to run it from the repository
root folder):

```
./scripts/rebase-release.sh 0.21
```

This will rebase the branch on latest master and force push the release branch.

## Tagging a release of a release branch

To tag a release, the `tag-release.sh` script should be used (NB: important to 
run it from the repository root folder):

```
./scripts/tag-release.sh 0.21 0.21.1-rc1
```

The script will go through the following steps, pausing after each to allow for cancelling the
operation:

1. Checkout release branch and update versions to tag version and commit to local copy
2. Create tag and push branch and tag to github

Travis will detect that the tag has been created, and build and push the release.

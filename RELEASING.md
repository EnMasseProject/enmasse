# Releasing EnMasse

EnMasse releases are created at semi-regular intervals, at present each month. This of course
depends a bit on the set of features targeted for the release, but in general EnMasse should be in a
'releasable state' at any point in time.

EnMasse uses semantic versioning of MAJOR.MINOR.MICRO versions with the following requirements:

* Major version is kept at 0 for the time being
* Minor versions should be upgrade/downgrade-compatible with the previous minor version
* Micro versions should only require an update to the images and not the templates

Each minor release has a release branch. Micro-releases are made off the same branch, with patches
committed to the minor release branch.

Each release is tagged with the MAJOR.MINOR.MICRO version.

Before a release, a release candidate is created and announced to allow the community some testing before the release is made.

Normally, a release is made within 24 hours after the release candidate is considered satisfactory.

## Update CHANGELOG.md

Add relevant changes to CHANGELOG.md.

## Creating a new release branch

The release branch should be on the form MAJOR.MINOR. The `scripts/branch-release.sh` script creates a release branch for a minor version. To create a new release branch, simply run it with the version name as argument (NB: important to run it from the repository root folder):

```
./scripts/branch-release.sh 0.22
```

The script will go through the following steps, pausing after each to allow for cancelling the operation.

1. Create local branch and update versions
2. Commit changes to local git repository
3. Push branch to the upstream repository

## Tagging a release

Minor and micro releases follow the same process for tagging a release. The only difference is that
a micro release will typically have the changes made to the release branch before tagging the new
micro.

To tag a release, the `scripts/tag-release.sh` script should be used (NB: important to 
run it from the repository root folder):

```
./scripts/tag-release.sh 0.21 0.21.1-rc1
```

The script will go through the following steps, pausing after each to allow for cancelling the
operation:

1. Checkout release branch and update versions to tag version and commit to local copy
2. Create tag and push branch and tag to github

Travis will detect that the tag has been created, and then build and publish the release.

## Updating documentation

* Checkout the [website documentation](https://github.com/EnMasseProject/enmasse.io)
* Add the new version to the documentation [build file](https://github.com/EnMasseProject/enmasse.io/blob/master/build.rb#L5)
* Push changes to website, travis CI will build and publish the new docs

## Update release tag changelog

Edit the release tag on github with the changes from CHANGELOG.md (Example: https://github.com/EnMasseProject/enmasse/releases/tag/0.26.0).

## Announce release

Send an e-mail to enmasse@redhat.com announcing the release, including changes for the release.

## Releasing to [OperatorHub](https://operatorhub.io/)

The EnMasse Operator is released as both a community-operators (for OpenShift) and upstream-community-operators (for Kubernetes).

You will find the Operator bundle beneath `install/olm` in the release artefacts produced by the release.

Use this [procedure](https://github.com/operator-framework/community-operators/blob/master/docs/testing-operators.md) to create a
new Operator Bundle version within the [EnMasse application registry](https://quay.io/new/?namespace=enmasse) and test it
in OperatorHub on both Kubernetes and OpenShift.

When versioning the bundle, use a `PACKAGE_VERSION` in the form `<EnMasseVersionNumber>-<Sequence Number>` e.g. 0.28.2-0.   If it is necessary
to republish the bundle, simply increment the sequence number.  Note it is currently not possible to remove versions from the application
registry on Quay.

Once complete raise two separate PR against the https://github.com/operator-framework/community-operators/ to update EnMasse in both the
community-operators and upstream-community-operators areas.



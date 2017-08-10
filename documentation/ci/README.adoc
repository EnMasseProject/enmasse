# EnMasse CI

EnMasse uses [Travis CI](http://travis-ci.org/) for continuous integration (CI). EnMasse is
contained in a [single repository](https://github.com/EnMasseProject/enmasse) which is build using
gradle.

Docker images are tagged with the commit hash of the build and pushed to [docker hub](https://hub.docker.com/r/enmasseproject/), and the latest successful snapshot of the EnMasse configuration is published on [bintray](https://dl.bintray.com/enmasse/snapshots/latest/enmasse-latest.tgz).

![Overview](ci.png)

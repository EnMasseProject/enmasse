# Building EnMasse

## Prerequisites

To build EnMasse, you need

    * Java JDK >= 1.8
    * GNU GCC C++
    * Ruby
    * Git

## Check out submodules

[jsonnet](http://jsonnet.org) is required to build templates. It is configured as a submodule that
can be initialized:
    
    git submodule update --init

## Building

#### For building and running EnMasse unit tests:

    ./gradlew build

If you want to rerun a task even if nothing has changed, append the `--rerun-tasks` flag to gradle.
To get more verbose information, append `-i`.

#### Build a docker image and push them to docker hub:

    DOCKER_ORG=myorg DOCKER_USER=myuser DOCKER_PASS=mypassword ./gradlew pack buildImage tagImage pushImage

#### Building a single module

    ./gradlew :topic-forwarder:build

#### Fast building of EnMasse and pushing images to docker registry in local OpenShift instance (avoids pushing to docker hub)

    DOCKER_REGISTRY=172.30.1.1:5000 DOCKER_ORG=myproject DOCKER_USER=developer DOCKER_PASS=`oc whoami -t` ./gradlew build pack buildImage tagImage pushImage -x test --parallel

#### Deploying to an OpenShift instance

    OPENSHIFT_MASTER=https://localhost:8443 OPENSHIFT_PROJECT=myproject OPENSHIFT_USER=developer OPENSHIFT_PASSWD=developer ./gradlew deploy

#### Running the systemtests

This assumes that the above deploy step has been run

    OPENSHIFT_MASTER_URL=https://localhost:8443 OPENSHIFT_NAMESPACE=myproject OPENSHIFT_TOKEN=`oc whoami -t` ./gradlew :systemtests:check -Psystemtests -i --rerun-tasks

## Reference

This is a reference of the different gradle tasks and options that can be set.

#### Gradle tasks

    * build       - build and unit test component
    * pack        - bundle component tarball artifact, ready to be added to docker image
    * buildImage  - builds docker image
    * tagImage    - tag docker image with registry and version (passed in environment)
    * pushImage   - push tagged image
    * tagVersion  - same as tagImage but uses the git tag (for releases) or latest
    * pushVersion - same as pushimage but uses git tag (for releases) or latest
    * deploy      - deploys the built templates to OpenShift. The images referenced by the template
                    must be available in a docker registry

Some of these tasks can be configured using environment variables as listed below.

#### Environment variables

There are several environment variables that control the behavior of the build. Some of them are
only consumed by some tasks:

    * OPENSHIFT\_MASTER  - URL to OpenShift master. Consumed by `deploy` and `:systemtest:check -Psystemtests` tasks
    * OPENSHIFT\_USER    - OpenShift user. Consumed by `deploy` task
    * OPENSHIFT\_PASSWD  - OpenShift password. Consumed by `deploy` task
    * OPENSHIFT\_TOKEN   - OpenShift token. Consumed by `systemtest` task
    * OPENSHIFT\_PROJECT - OpenShift project for EnMasse. Consumed by `deploy` and `systemtests` tasks
    * DOCKER\_ORG        - Docker organization for EnMasse images. Consumed by `build`, `pack`, `*Image` and `*Version` tasks. Defaults to `enmasseproject`
    * DOCKER\_USER       - Docker user for registry login. Consumed by `push*` tasks
    * DOCKER\_PASS       - Docker password for registry login. Consumed by `push*` tasks
    * DOCKER\_REGISTRY   - Docker registry for EnMasse images. Consumed by `build`, `pack`, `tag*` and `push*`. Defaults to `docker.io`
    * COMMIT             - Commit hash used as docker image tag in snapshots and in test. Consumed by all tasks except `deploy`. Defaults to `latest`
    * VERSION            - Version used when building releases. Consumed by all tasks except `deploy`. Defaults to `latest`

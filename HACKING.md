# Building EnMasse

## Prerequisites

To build EnMasse, you need

    * Java JDK >= 1.8
    * GNU GCC C++
    * Python
    * Git
    * Node.js
    * npm
    * docker
    * maven

The EnMasse java modules are built using gradle. Node modules are built using make. Docker images
are built using make.

## Check out submodules

[jsonnet](http://jsonnet.org) is required to build templates. It is configured as a submodule that
can be initialized:
    
    git submodule update --init
    
## Building

### Pre-installation

   * `npm install -g mocha-junit-reporter`
   * `npm install -g rhea`
   * `npm install -g debug`

*Note*: Make sure docker daemon is in running state.

#### For building and running EnMasse unit tests and building docker image:

    make

This can be run at the top level or within each module. You can also run the 'build', 'test', and 'package' targets individually.
This builds all modules including java.

#### Build a docker image and push them to docker hub:

    export DOCKER_ORG=myorg
    docker login -u myuser -p mypassword docker.io
    make docker_build
    make docker_tag docker_push

#### Fast building of EnMasse and pushing images to docker registry in local OpenShift instance (avoids pushing to docker hub)

    export DOCKER_ORG=myproject
    export DOCKER_REGISTRY=172.30.1.1:5000
    docker login -u myproject -p `oc whoami -t` 172.30.1.1:5000
    make GRADLE_ARGS="-x test" -j4
    make docker_tag docker_push

#### Deploying to an OpenShift instance

    make deploy

#### Running the systemtests

This assumes that the above deploy step has been run

    make systemtests

## Reference

This is a reference of the different gradle tasks and options that can be set.

#### Make targets

    * `build`        - build
    * `test`         - run tests
    * `package`      - create artifact bundle
    * `docker_build` - build docker image
    * `docker_tag`   - tag docker image
    * `docker_push`  - push docker image
    * `deploy`       - deploys the built templates to OpenShift. The images referenced by the template must be available in a docker registry
    * `systemtests`  - run systemtests

Some of these tasks can be configured using environment variables as listed below.

#### Environment variables

There are several environment variables that control the behavior of the build. Some of them are
only consumed by some tasks:

    * OPENSHIFT_MASTER  - URL to OpenShift master. Consumed by `deploy` and `systemtests` targets
    * OPENSHIFT_USER    - OpenShift user. Consumed by `deploy` target
    * OPENSHIFT_PASSWD  - OpenShift password. Consumed by `deploy` target
    * OPENSHIFT_TOKEN   - OpenShift token. Consumed by `systemtests` target
    * OPENSHIFT_PROJECT - OpenShift project for EnMasse. Consumed by `deploy` and `systemtests` targets
    * DOCKER_ORG        - Docker organization for EnMasse images. Consumed by `build`, `package`, `docker*` targets. tasks. Defaults to `enmasseproject`
    * DOCKER_REGISTRY   - Docker registry for EnMasse images. Consumed by `build`, `package`, `docker_tag` and `docker_push` targets. Defaults to `docker.io`
    * TAG               - Tag used as docker image tag in snapshots and in the generated templates. Consumed by `build`, `package`, `docker_tag` and `docker_push` targets.

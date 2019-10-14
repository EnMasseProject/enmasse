# Systemtests

This repository contains the EnMasse system tests. The tests can be run against an EnMasse instance
running on Kubernetes or OpenShift. The make targets will attempt to use `oc` command line tool and
fallback to `kubectl`.

## Setting up test environment

*NOTE*: On OKD, make sure `oc` is logged in to your cluster with `cluster-admin` privileges. However you should not run the tests with `system:admin` user. You can give the privileges to a standard user :

    oc login -u system:admin https://localhost:8443
    oc adm policy add-cluster-role-to-user cluster-admin developer
    oc login -u developer -p developer https://localhost:8443

Systemtests framework installs enmasse and iot operators, you only need to generate templates and store them into templates/build/enmasse-latest
or you need to provide custom path to your install bundle via env variable TEMPLATES

#### Generate templates

    make templates

#### Provide custom template path

    export TEMPLATES="/path/to/your/install/bundle"

## Running all tests

    make SYSTEMTEST_PROFILE=${PROFILE)

##  Running a single test class

    make SYSTEMTEST_PROFILE=${PROFILE) SYSTEMTEST_ARGS="**.SmokeTest"

Where $PROFILE can be:
* systemtests
* marathon
* iot
* shared
* isolated
* shared-iot
* isolated-iot
* smoke
* smoke-iot
* upgrade

## Running upgrade test

    mkdir templates/build -pv
    export START_VERSION=0.28.2
    wget https://github.com/EnMasseProject/enmasse/releases/download/${START_VERSION}/enmasse-${START_VERSION}.tgz -O templates/build/enmasse-${START_VERSION}.tgz
    tar zxvf templates/build/enmasse-${version}.tgz -C templates/build
    make templates
    export START_TEMPLATES=${pwd}/templates/build/enmasse-${START_VERSION}
    export UPGRADE_TEMPLATES=${pwd}/templates/build/enmasse-latest
    make SYSTEMTEST_PROFILE=upgrade

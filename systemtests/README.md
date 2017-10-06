# Systemtests

[![Build Status](https://travis-ci.org/EnMasseProject/systemtests.svg?branch=master)](https://travis-ci.org/EnMasseProject/systemtests)

This repository contains the EnMasse system tests. The tests can be run against an EnMasse instance
running on OpenShift.

## Setting up test environment

It is assumed that you have EnMasse running on an OpenShift instance already. If not, have a look at
the [getting started](https://github.com/EnMasseProject/enmasse/tree/master/getting-started) guide.

To setup test environment:

    oc login https://localhost:8443
    source scripts/ocenv.sh localhost myproject developer

Replace `localhost` `myproject` and `developer` with the desired values for your OpenShift instance.

## Running all tests

    mvn test -Psystemtests

##  Running a single test class

    mvn test -Psystemtests -Dtest=SmokeTest

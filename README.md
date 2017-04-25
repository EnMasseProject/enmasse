# Artemis image for EnMasse

[![Build Status](https://travis-ci.org/EnMasseProject/artemis-image.svg?branch=master)](https://travis-ci.org/EnMasseProject/artemis-image)

This repository contains
   * Dockerfile for building container image
   * Startup scripts and configuration for Artemis running in containers
   * Shutdown hooks for use when running in OpenShift
   * Launcher for blocking SIGTERM in OpenShift


## Build instructions

    git submodule update --init
    gradle buildArtemis assemble build buildTar downloadArtemis

    docker build -t enmasseproject/artemis .



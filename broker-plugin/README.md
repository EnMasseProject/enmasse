# Artemis image for EnMasse

This repository contains
   * Dockerfile for building container image
   * Startup scripts and configuration for Artemis running in containers
   * Shutdown hooks for use when running in OpenShift
   * Launcher for blocking SIGTERM in OpenShift


## Build instructions

    git submodule update --init

    make



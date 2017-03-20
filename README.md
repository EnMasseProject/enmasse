[![Build Status](https://travis-ci.org/EnMasseProject/admin.svg?branch=master)](https://travis-ci.org/EnMasseProject/admin)

This repository contains all administrative components in EnMasse.

## Address Controller

The Address Controller is a global multitenant components that implements an API for creating and deleting EnMasse infrastructure instances as well as deploying and modifying the address configuration per instance. It combines the address configuration with the flavor configuration to find the appropriate OpenShift template to use for a given address, and instantiates the template with parameters as specified by the flavor.

The Address Controller exposes an API for modifying the addressing config using either AMQP or HTTP.

## Configuration Server

This repository contains the config server. The config server is able to watch OpenShift resources and notify AMQP clients when they change. The subscription addresses available are:

* maas: Creating a receiver with this as the source will provide updates whenever the addressing config of EnMasse changes
* podsense: Discover pods with a given label set

## Queue Scheduler

The queue scheduler is responsible for allocating queues to brokers. 

# Build instructions

    gradle build


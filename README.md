# Configuration Server

[![Build Status](https://travis-ci.org/EnMasseProject/configserv.svg?branch=master)](https://travis-ci.org/EnMasseProject/configserv)

This repository contains the config server.  The config server is able to watch OpenShift resources and notify AMQP clients when they change. The subscription addresses available are:

* maas: Creating a receiver with this as the source will provide updates whenever the addressing config of EnMasse changes

## Build instructions

    gradle build


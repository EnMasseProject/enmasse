## Configuration Server

This repository contains the config server. The config server is able to watch OpenShift resources and notify AMQP clients when they change. The subscription addresses available are:

* v1/addresses: Creating a receiver with this as the source will provide updates whenever the addressing config of EnMasse changes
* podsense: Discover pods with a given label set

# Build instructions

    make

# Topic forwarder

[![Build Status](https://travis-ci.org/EnMasseProject/topic-forwarder.svg?branch=master)](https://travis-ci.org/EnMasseProject/topic-forwarder)

The topic-forwarder is an AMQP message forwarder for topics in EnMasse, currently used with the
Artemis broker. The forwarder discovers other brokers using the podsense discovery service, and creates a
durable subscription to the local broker. Once created, it establishes a connection to other brokers
in the cluster and forwards all messages to them.

# Build instructions

    make

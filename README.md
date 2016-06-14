[![Build Status](https://travis-ci.org/EnMasseProject/configmap-bridge.svg?branch=master)](https://travis-ci.org/EnMasseProject/configmap-bridge)

# ConfigMap Bridge

The configmap bridge is a server that subscribes to a configmap in openshift (or kubernetes), and allows clients to subscribe for configmap updates using different protocols. Currently, only AMQP is supported.

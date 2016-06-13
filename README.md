[![Build Status](https://travis-ci.org/EnMasseProject/rc-generator.svg?branch=master)](https://travis-ci.org/EnMasseProject/rc-generator)

# ReplicationController (RC) Generator

The RC generator is responsible for converting a simple address configuration to a set of
replication controllers for the different addresses. It can be run as a standlone tool, or as an
agent that subscribes to the config through the configmap-bridge.

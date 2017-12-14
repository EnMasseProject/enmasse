#!/bin/sh
docker run -e AUTHENTICATION_SERVICE_HOST=127.0.0.1 -e AUTHENTICATION_SERVICE_PORT=12345 -e ADDRESS_SPACE_TYPE=brokered -v $PWD/metrics:/etc/prometheus-config:z -v $PWD/certs:/etc/authservice-ca:z -v $PWD/certs:/etc/enmasse-certs:z --net=host -ti enmasse-artemis:latest

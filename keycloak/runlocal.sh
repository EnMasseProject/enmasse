#!/bin/sh
docker run -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -v $PWD/cert:/opt/jboss/keycloak/standalone/cert:z --net=host -ti enmasse-keycloak:latest

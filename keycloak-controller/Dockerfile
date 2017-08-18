FROM enmasseproject/java-base:8

ARG version=latest
ENV VERSION ${version}
ADD build/distributions/keycloak-controller.tgz /

CMD /keycloak-controller/bin/keycloak-controller

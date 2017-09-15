FROM jboss/keycloak:3.2.1.Final

ARG version=latest

ADD ./sasl-plugin/build/libs/sasl-plugin-all.jar /opt/jboss/keycloak/providers/
ADD ./src/main/xsl/*.xsl /opt/jboss/keycloak/
ADD ./src/main/sh/init-keycloak.sh /opt/jboss/keycloak/bin/

USER root
RUN /opt/jboss/keycloak/bin/init-keycloak.sh
USER jboss
ENV JAVA_OPTS "-Dvertx.cacheDirBase=/tmp -Djboss.bind.address=0.0.0.0 -Djava.net.preferIPv4Stack=true"

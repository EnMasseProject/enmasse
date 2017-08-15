FROM jboss/keycloak:3.2.1.Final

ARG version=latest

ADD ./sasl-plugin/build/libs/sasl-plugin-all.jar /opt/jboss/keycloak/providers/
ADD ./src/main/xsl/*.xsl /opt/jboss/keycloak/
ADD ./src/main/sh/init-keycloak.sh ./src/main/sh/start-keycloak.sh /opt/jboss/keycloak/bin/

USER root
RUN /opt/jboss/keycloak/bin/init-keycloak.sh
USER jboss

ENTRYPOINT [ "/opt/jboss/keycloak/bin/start-keycloak.sh" ]

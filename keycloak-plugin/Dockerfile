FROM enmasseproject/java-base:8-6

RUN yum -y install openssl && yum -y clean all

ARG version=latest
ENV VERSION ${version}
ENV KEYCLOAK_PLUGIN_DIR /keycloak-plugin

ADD ./build/keycloak-plugin-${VERSION}.tar.gz ${KEYCLOAK_PLUGIN_DIR}/

ENTRYPOINT [ "sh", "-c", "${KEYCLOAK_PLUGIN_DIR}/bin/init-keycloak.sh" ]

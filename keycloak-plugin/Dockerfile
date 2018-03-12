FROM enmasseproject/java-base:8-6

ARG version=latest
ENV VERSION ${version}
ENV KEYCLOAK_PLUGIN_DIR /keycloak-plugin

ADD ./build/keycloak-plugin-${VERSION}.tar.gz ${KEYCLOAK_PLUGIN_DIR}/

RUN yum -y install openssl && yum -y update && yum -y clean all

ENTRYPOINT [ "sh", "-c", "${KEYCLOAK_PLUGIN_DIR}/bin/init-keycloak.sh" ]

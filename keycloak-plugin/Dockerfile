FROM enmasseproject/java-base:8-10

RUN yum -y install openssl && yum -y clean all

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ENV KEYCLOAK_PLUGIN_DIR /keycloak-plugin

ADD ./build/keycloak-plugin-${VERSION}.tar.gz ${KEYCLOAK_PLUGIN_DIR}/

ENTRYPOINT [ "sh", "-c", "${KEYCLOAK_PLUGIN_DIR}/bin/init-keycloak.sh" ]

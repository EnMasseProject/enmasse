FROM quay.io/enmasse/java-base:11-4

ARG version
ARG maven_version
ARG commit

ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ENV KEYCLOAK_PLUGIN_DIR /keycloak-plugin

ADD ./build/keycloak-plugin-${maven_version}.tar.gz ${KEYCLOAK_PLUGIN_DIR}/

USER 1000

ENTRYPOINT [ "sh", "-c", "${KEYCLOAK_PLUGIN_DIR}/bin/init-keycloak.sh" ]

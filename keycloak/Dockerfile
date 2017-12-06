FROM jboss/keycloak:3.3.0.Final

ARG version=latest
ENV VERSION ${version}

ADD ./build/keycloak-${VERSION}.tar.gz ${JBOSS_HOME}/

USER root
RUN yum -y install openssl && yum -y update && yum -y clean all
RUN ${JBOSS_HOME}/bin/init-keycloak.sh
USER jboss
ENV JAVA_OPTS "-Dvertx.cacheDirBase=/tmp -Djboss.bind.address=0.0.0.0 -Djava.net.preferIPv4Stack=true -Xms512m -Xmx512m"
ENTRYPOINT [ "sh", "-c", "${JBOSS_HOME}/bin/start-keycloak.sh" ]

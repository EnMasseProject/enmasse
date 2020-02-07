FROM quay.io/enmasse/java-base:11-3

RUN microdnf install which python gettext hostname iputils && microdnf clean all && mkdir -p /var/run/artemis/

ARG version
ARG maven_version
ARG commit
ENV ARTEMIS_HOME=/opt/apache-artemis PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD ./build/broker-plugin-${maven_version}-dist.tar.gz /

RUN chgrp -R 0 /opt/broker-plugin && \
    chmod -R g=u /opt/broker-plugin

CMD ["/opt/broker-plugin/bin/init-broker.sh"]

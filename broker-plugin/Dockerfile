FROM quay.io/enmasse/java-base:11-5

RUN microdnf install which python gettext hostname iputils libxslt && microdnf clean all && mkdir -p /var/run/artemis/

ARG version
ARG maven_version
ARG revision
ENV ARTEMIS_HOME=/opt/apache-artemis PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD ./plugin/target/plugin-${maven_version}-dist.tar.gz /

RUN chgrp -R 0 /opt/broker-plugin && \
    chmod -R g=u /opt/broker-plugin

CMD ["/opt/broker-plugin/bin/init-broker.sh"]

FROM centos:8

ARG version
ARG maven_version
ARG revision
ENV ARTEMIS_HOME=/opt/apache-artemis PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD ./plugin/target/plugin-${maven_version}-dist.tar.gz /

RUN dnf install -y gettext hostname && dnf clean all -y && chgrp -R 0 /opt/broker-plugin && chmod -R g=u /opt/broker-plugin

CMD ["/opt/broker-plugin/bin/init-broker.sh"]

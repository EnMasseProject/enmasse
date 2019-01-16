FROM enmasseproject/java-base:8-11

RUN yum -y install which python gettext hostname iputils openssl && yum clean all -y && mkdir -p /var/run/artemis/

ARG version
ARG commit
ENV ARTEMIS_HOME=/opt/apache-artemis PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version} COMMIT=${commit}

ADD ./build/broker-plugin-${version}-dist.tar.gz /
ADD ./tcnative/target/lib/netty-tcnative-boringssl-static-2.0.7.Final.jar /opt/broker-plugin/lib/
ADD ./jmx_exporter/target/lib/jmx_prometheus_javaagent-0.1.0.jar /opt/broker-plugin/jmx_exporter/

RUN chgrp -R 0 /opt/broker-plugin && \
    chmod -R g=u /opt/broker-plugin

CMD ["/opt/broker-plugin/bin/init-broker.sh"]

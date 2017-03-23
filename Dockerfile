FROM fedora:25

RUN dnf -y install which java-1.8.0-openjdk libaio python gettext hostname iputils && dnf clean all -y && mkdir -p /var/run/artemis/

ARG version=latest
ENV ARTEMIS_HOME=/opt/apache-artemis-2.0.0-SNAPSHOT PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version}

ADD ./build/apache-artemis-bin.tar.gz ./artemis-shutdown-hook/build/distributions/artemis-shutdown-hook.tar /opt/

COPY ./activemq-artemis/integration/activemq-amqp-connector/target/artemis-amqp-connector-2.0.0-SNAPSHOT.jar $ARTEMIS_HOME/lib

# Needed for bridge clustering
# COPY ./artemis-plugin/build/libs/artemis-plugin.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/kubernetes-0.9.0.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/common-0.9.0.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/jboss-dmr-1.3.0.Final.jar $ARTEMIS_HOME/lib

COPY ./utils/* ./artemis-launcher/build/exe/main/main $ARTEMIS_HOME/bin/
COPY ./config_templates /config_templates

VOLUME /var/run/artemis

EXPOSE 5673 61616 8161

# Needed for bridge clustering
# EXPOSE 7800 7801 7802

CMD ["/opt/apache-artemis-2.0.0-SNAPSHOT/bin/main", "/opt/apache-artemis-2.0.0-SNAPSHOT/bin/launch.sh"]

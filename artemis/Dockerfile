FROM fedora:25

RUN dnf -y install which java-1.8.0-openjdk libaio python gettext hostname iputils openssl && dnf clean all -y && mkdir -p /var/run/artemis/

ARG version=latest
ENV ARTEMIS_HOME=/opt/apache-artemis-2.4.0 PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version}

ADD ./build/apache-artemis-bin.tar.gz /opt
ADD ./build/artemis-image-${version}.tar.gz /

# Needed for bridge clustering
# COPY ./artemis-plugin/build/libs/artemis-plugin.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/kubernetes-0.9.0.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/common-0.9.0.jar $ARTEMIS_HOME/lib
# COPY ./artemis-plugin/build/libs/jboss-dmr-1.3.0.Final.jar $ARTEMIS_HOME/lib

VOLUME /var/run/artemis

# Needed for bridge clustering
# EXPOSE 7800 7801 7802

CMD ["/opt/apache-artemis-2.4.0/bin/launch.sh"]

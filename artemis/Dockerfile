FROM enmasseproject/java-base:8-7

RUN yum -y install which libaio python gettext hostname iputils openssl && yum clean all -y && mkdir -p /var/run/artemis/

ARG version
ARG commit
ENV ARTEMIS_HOME=/opt/apache-artemis-2.6.2 PATH=$ARTEMIS_HOME/bin:$PATH VERSION=${version} COMMIT=${commit}

ADD ./build/apache-artemis-bin.tar.gz /opt
ADD ./build/artemis-image-${version}.tar.gz /

VOLUME /var/run/artemis

CMD ["/opt/apache-artemis-2.6.2/bin/launch.sh"]

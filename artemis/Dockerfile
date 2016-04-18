FROM fedora:23

RUN dnf -y install which java-1.8.0-openjdk libaio python && dnf clean all -y

ENV ARTEMIS_VERSION 1.3.0-SNAPSHOT
ENV ARTEMIS_HOME /opt/apache-artemis-$ARTEMIS_VERSION
ADD apache-artemis-$ARTEMIS_VERSION-bin.tar.gz /opt
COPY ./run_artemis.sh ./get_free_instance.py $ARTEMIS_HOME/bin/
ENV PATH $ARTEMIS_HOME/bin:$PATH

RUN mkdir /var/run/artemis/
VOLUME /var/run/artemis

EXPOSE 5672
CMD ["run_artemis.sh"]

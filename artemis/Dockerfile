FROM fedora:23

RUN dnf -y install which java-1.8.0-openjdk libaio python sed && dnf clean all -y

ENV ARTEMIS_HOME /opt/apache-artemis-1.4.0-SNAPSHOT
ADD apache-artemis-1.4.0-20160620.113416-8-bin.tar.gz /opt
COPY ./run_artemis.sh ./get_free_instance.py $ARTEMIS_HOME/bin/
ENV PATH $ARTEMIS_HOME/bin:$PATH

RUN mkdir /var/run/artemis/
VOLUME /var/run/artemis

EXPOSE 5673
CMD ["run_artemis.sh"]

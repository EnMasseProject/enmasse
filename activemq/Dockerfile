FROM fedora:23

RUN dnf -y install which java-1.8.0-openjdk libaio python gettext procps-ng sed && dnf clean all -y

ENV ACTIVEMQ_HOME /opt/apache-activemq-5.13.3
ADD apache-activemq-5.13.3-bin.tar.gz /opt
COPY ./run_amq.sh ./activemq.xml.template /
ENV PATH $ACTIVEMQ_HOME/bin:$PATH
ENV ACTIVEMQ_DATA /tmp/amq
RUN chmod -R 777 /opt

EXPOSE 5673
CMD ["/run_amq.sh"]

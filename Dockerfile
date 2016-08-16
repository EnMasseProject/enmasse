FROM fedora:23

RUN dnf -y install which java-1.8.0-openjdk libaio python gettext hostname iputils && dnf clean all -y

ENV ARTEMIS_HOME /opt/apache-artemis-1.4.0-SNAPSHOT
ENV PATH $ARTEMIS_HOME/bin:$PATH

ADD apache-artemis-bin.tar.gz /opt
ADD ./artemis-shutdown-hook/build/distributions/artemis-shutdown-hook.tar /

COPY ./artemis-plugin/lib/artemis-plugin.jar $(ARTEMIS_HOME)/lib
COPY ./artemis-plugin/lib/kubernetes-0.9.0.jar $(ARTEMIS_HOME)/lib
COPY ./artemis-plugin/lib/common-0.9.0.jar $(ARTEMIS_HOME)/lib
COPY ./artemis-plugin/lib/jboss-dmr-1.3.0.Final.jar $(ARTEMIS_HOME)/lib

COPY ./utils/run_artemis.sh ./utils/get_free_instance.py $ARTEMIS_HOME/bin/
COPY ./config_templates /config_templates
COPY ./artemis-launcher/build/binaries/mainExecutable/main /launcher

RUN mkdir /var/run/artemis/
VOLUME /var/run/artemis

EXPOSE 5673
EXPOSE 7800
EXPOSE 7801
EXPOSE 7802
EXPOSE 61616

CMD ["/launcher", "/opt/apache-artemis-1.4.0-SNAPSHOT/bin/run_artemis.sh"]

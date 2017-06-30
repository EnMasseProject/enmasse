FROM centos:7

RUN yum -y install java-1.8.0-openjdk-devel && yum clean all
ENV JAVA_HOME /usr/lib/jvm/java

ARG version=1.0-SNAPSHOT
ADD build/mqtt-gateway.tgz /

EXPOSE 1883 8883

CMD ["/run_mqtt.sh"]

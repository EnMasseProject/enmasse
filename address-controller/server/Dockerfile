FROM centos:7

RUN yum -y install java-1.8.0-openjdk-devel && yum clean all
ENV JAVA_HOME /usr/lib/jvm/java

ARG version=latest
ENV VERSION ${version}
ADD build/distributions/server-${version}.tar /

EXPOSE 5672
EXPOSE 8080

CMD /server-${VERSION}/bin/server

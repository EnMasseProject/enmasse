FROM enmasseproject/java-base:8

RUN yum -y install openssl && yum -y clean all

ARG version=latest
ENV VERSION ${version}
ADD server/build/distributions/server.tgz /

EXPOSE 5672
EXPOSE 8080

CMD /server/bin/server

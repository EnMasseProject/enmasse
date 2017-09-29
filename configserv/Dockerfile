FROM enmasseproject/java-base:8

ARG version=latest
ENV VERSION ${version}
ADD build/distributions/configserv.tgz /

EXPOSE 5672

CMD /configserv/bin/configserv

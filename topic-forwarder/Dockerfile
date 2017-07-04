FROM enmasseproject/java-base:8

ARG version=latest
ENV VERSION ${version}
ADD build/distributions/topic-forwarder.tgz /

EXPOSE 8080

CMD /topic-forwarder/bin/topic-forwarder

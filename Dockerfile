FROM openjdk:8-jre

ADD build/distributions/topic-forwarder.tar /

EXPOSE 8080

CMD ["/topic-forwarder/bin/topic-forwarder"]

FROM openjdk:8-jre

ADD forwarder/build/distributions/forwarder.tar /

EXPOSE 8080

CMD ["/forwarder/bin/forwarder"]

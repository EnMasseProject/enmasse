FROM java:8

ADD forwarder/build/distributions/forwarder.tar /

EXPOSE 8080

CMD ["/forwarder/bin/forwarder"]

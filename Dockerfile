FROM java:8

ADD forwarder/build/distributions/forwarder.tar /

CMD ["/forwarder/bin/forwarder"]

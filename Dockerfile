FROM openjdk:8-jre

ADD build/distributions/configserv.tar.gz /

EXPOSE 5672

CMD ["/configserv/bin/configserv"]

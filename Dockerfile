FROM openjdk:8-jre

ADD build/distributions/configserv.tar /

EXPOSE 5672

CMD ["/configserv/bin/configserv"]

FROM openjdk:8-jre

ADD build/distributions/configuration-service.tar /

EXPOSE 5672

CMD ["/configuration-service/bin/configuration-service"]

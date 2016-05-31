FROM java:8

ADD target/config-subscription-service-1.0-SNAPSHOT-jar-with-dependencies.jar /config-subscription-service.jar
ADD src/main/sh/config-subscription-service.sh /config-subscription-service

EXPOSE 5672

CMD ["/config-subscription-service"]

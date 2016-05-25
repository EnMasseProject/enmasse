FROM java:8

ADD target/config-subscription-service-1.0-SNAPSHOT-jar-with-dependencies.jar /config-subscription-service.jar
ADD src/main/sh/config-subscription-service.sh /config-subscription-service

ENV OPENSHIFT_USER test
ENV OPENSHIFT_TOKEN N8DGnOuytqoDIJqk4DA7DAeiO37fEyiONH8w4EqgMis
ENV OPENSHIFT_NAMESPACE petproject

EXPOSE 5672

CMD ["/config-subscription-service"]

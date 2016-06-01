FROM java:8

ADD build/distributions/config-subscription-service-0.1.tar /
ADD src/main/sh/config-subscription-service.sh /config-subscription-service

EXPOSE 5672

CMD ["/config-subscription-service"]

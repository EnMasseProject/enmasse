FROM java:8

ADD build/distributions/config-subscription-service-0.1.tar /

EXPOSE 5672

CMD ["/config-subscription-service-0.1/bin/config-subscription-service"]

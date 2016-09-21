FROM openjdk:8-jre

ADD build/distributions/configmap-bridge.tar /

EXPOSE 5672

CMD ["/configmap-bridge/bin/configmap-bridge"]

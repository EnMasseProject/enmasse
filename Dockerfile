FROM java:8

ADD build/distributions/configmap-bridge.tar /

EXPOSE 5672

CMD ["/configmap-bridge/bin/configmap-bridge"]

FROM java:8

ADD build/distributions/configmap-bridge-0.1.tar /

EXPOSE 5672

CMD ["/configmap-bridge-0.1/bin/configmap-bridge"]

FROM openjdk:8-jre-alpine

ARG version=1.0-SNAPSHOT
ADD target/mqtt-gateway-${version}-bin.tar.gz /

EXPOSE 1883 8883

CMD ["/run_mqtt.sh"]

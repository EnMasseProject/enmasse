FROM openjdk:8-jre-alpine

ADD target/mqtt-gateway-1.0-SNAPSHOT.jar /
COPY ./run_mqtt.sh /etc/mqtt-gateway/

EXPOSE 1883 8883

CMD ["/etc/mqtt-gateway/run_mqtt.sh"]

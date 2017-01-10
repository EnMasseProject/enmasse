FROM openjdk:8-jre-alpine

ADD target/mqtt-frontend-1.0-SNAPSHOT.jar /
COPY ./run_mqtt.sh /etc/mqtt-frontend/

EXPOSE 1883 8883

CMD ["/etc/mqtt-frontend/run_mqtt.sh"]

FROM openjdk:8-jre-alpine

ADD target/mqtt-lwt-1.0-SNAPSHOT.jar /
COPY ./run_mqtt.sh /etc/mqtt-lwt/

CMD ["/etc/mqtt-lwt/run_mqtt.sh"]
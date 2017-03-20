FROM openjdk:8-jre-alpine

ARG version=1.0-SNAPSHOT
ADD target/mqtt-lwt-${version}.jar /
COPY ./run_mqtt.sh /etc/mqtt-lwt/

CMD ["/etc/mqtt-lwt/run_mqtt.sh"]

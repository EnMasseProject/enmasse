FROM enmasseproject/java-base:8

ARG version=1.0-SNAPSHOT
ADD build/mqtt-gateway.tgz /

EXPOSE 1883 8883

CMD ["/run_mqtt.sh"]

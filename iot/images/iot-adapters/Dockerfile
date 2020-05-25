FROM quay.io/enmasse/java-base:11-5

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD \
  target/dependency/iot-amqp-adapter.jar \
  target/dependency/iot-http-adapter.jar \
  target/dependency/iot-lorawan-adapter.jar \
  target/dependency/iot-mqtt-adapter.jar \
  target/dependency/iot-sigfox-adapter.jar \
  /

ENV JAVA_LAUNCH_PROFILE=openjdk-11

ENTRYPOINT ["/opt/run-java/launch_java.sh", "-Dloader.path=/extensions/", "-jar"]

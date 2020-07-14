FROM quay.io/ctronenmassetesting/java-base:11-6

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD \
  target/dependency/iot-device-connection-infinispan.jar \
  target/dependency/iot-device-registry-infinispan.jar \
  \
  target/dependency/iot-device-registry-jdbc.jar \
  /

ENV JAVA_LAUNCH_PROFILE=openjdk-11

ENTRYPOINT ["/opt/run-java/launch_java.sh", "-Dloader.path=/extensions/", "-jar"]

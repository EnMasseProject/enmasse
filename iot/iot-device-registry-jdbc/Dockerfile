FROM quay.io/enmasse/java-base:11-4

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/iot-device-registry-jdbc-${MAVEN_VERSION}.jar /iot-device-registry-jdbc.jar

ENV JAVA_LAUNCH_PROFILE=openjdk-11
CMD ["/opt/run-java/launch_java.sh", "/iot-device-registry-jdbc.jar"]

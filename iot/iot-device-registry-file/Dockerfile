FROM enmasseproject/java-base:11-0

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/iot-device-registry-file-${MAVEN_VERSION}.jar /iot-device-registry-file.jar

CMD ["/opt/run-java/launch_java.sh", "/iot-device-registry-file.jar"]

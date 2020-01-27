FROM quay.io/ctronenmassetesting/java-base:11-5

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/iot-device-registry-jdbc-${maven_version}.jar /iot-device-registry-jdbc.jar

ENV JAVA_LAUNCH_PROFILE=openjdk-11
CMD ["/opt/run-java/launch_java.sh", "-Dloader.path=/extensions/", "-jar", "/iot-device-registry-jdbc.jar"]

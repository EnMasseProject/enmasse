FROM quay.io/enmasse/java-base:11-1

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/iot-tenant-cleaner-${maven_version}.jar /iot-tenant-cleaner.jar

CMD ["/opt/run-java/launch_java.sh", "/iot-tenant-cleaner.jar"]
FROM quay.io/enmasse/java-base:11-5

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD target/iot-tenant-cleaner-${maven_version}.jar /iot-tenant-cleaner.jar

ENV JAVA_LAUNCH_PROFILE=openjdk-11
CMD ["/opt/run-java/launch_java.sh", "-cp", "/iot-tenant-cleaner.jar:/extensions/*", "io.enmasse.iot.tools.cleanup.Application"]

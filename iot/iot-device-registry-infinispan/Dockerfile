FROM enmasseproject/java-base:11-1

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/iot-device-registry-infinispan-${MAVEN_VERSION}.jar /iot-device-registry-infinispan.jar

CMD ["/opt/run-java/launch_java.sh", "/iot-device-registry-infinispan.jar"]

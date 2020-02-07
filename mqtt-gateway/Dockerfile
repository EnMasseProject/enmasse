FROM quay.io/enmasse/java-base:11-3

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/mqtt-gateway-${maven_version}.jar /mqtt-gateway.jar

CMD ["/opt/run-java/launch_java.sh", "/mqtt-gateway.jar"]

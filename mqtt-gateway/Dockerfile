FROM quay.io/enmasse/java-base:11-5

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}
ADD target/mqtt-gateway-${maven_version}.jar /mqtt-gateway.jar

CMD ["/opt/run-java/launch_java.sh", "-jar", "/mqtt-gateway.jar"]

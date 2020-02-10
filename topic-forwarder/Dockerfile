FROM quay.io/enmasse/java-base:11-4

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/topic-forwarder-${maven_version}.jar /topic-forwarder.jar

CMD ["/opt/run-java/launch_java.sh", "/topic-forwarder.jar"]

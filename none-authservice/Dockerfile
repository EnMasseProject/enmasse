FROM quay.io/enmasse/java-base:11-1

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/none-authservice-${MAVEN_VERSION}.jar /none-authservice.jar

CMD ["/opt/run-java/launch_java.sh", "/none-authservice.jar"]

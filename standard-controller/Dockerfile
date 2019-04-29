FROM quay.io/enmasse/java-base:11-1

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/standard-controller-${MAVEN_VERSION}-dist.tar.gz /

CMD ["/opt/run-java/launch_java.sh", "/standard-controller.jar"]

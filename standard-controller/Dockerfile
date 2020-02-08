FROM quay.io/enmasse/java-base:11-3

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/standard-controller-${maven_version}-dist.tar.gz /

CMD ["/opt/run-java/launch_java.sh", "/opt/standard-controller.jar"]

FROM quay.io/enmasse/java-base:11-5

ARG version
ARG revision
ARG maven_version
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD target/address-space-controller-${maven_version}-dist.tar.gz /

ENV LOG_LEVEL info

CMD ["/opt/run-java/launch_java.sh", "-jar", "/opt/address-space-controller.jar"]

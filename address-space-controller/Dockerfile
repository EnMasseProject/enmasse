FROM quay.io/enmasse/java-base:11-1

RUN yum -y install openssl && yum -y clean all

ARG version
ARG commit
ARG maven_version
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/address-space-controller-${maven_version}-dist.tar.gz /

ENV LOG_LEVEL info

CMD ["/opt/run-java/launch_java.sh", "/opt/address-space-controller.jar"]

FROM enmasseproject/java-base:8-11

RUN yum -y install openssl && yum -y clean all

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD target/address-space-controller-${VERSION}-dist.tar.gz /

ENV LOG_LEVEL info

CMD ["/opt/run-java/launch_java.sh", "/address-space-controller.jar"]

FROM enmasseproject/java-base:8-6

RUN yum -y install openssl && yum -y clean all

ARG version=latest
ENV VERSION ${version}
ADD target/address-controller-${VERSION}.jar /address-controller.jar
ENV JAVA_OPTS "-DLOG_LEVEL=info"

CMD ["/opt/run-java/launch_java.sh", "/address-controller.jar"]

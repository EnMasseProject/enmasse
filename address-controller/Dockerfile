FROM enmasseproject/java-base:8-4

RUN yum -y install openssl && yum -y clean all

ARG version=latest
ENV VERSION ${version}
ADD target/address-controller-${VERSION}.jar /address-controller.jar

CMD ["/opt/run-java/launch_java.sh", "/address-controller.jar"]
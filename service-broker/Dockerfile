FROM enmasseproject/java-base:8-6

ARG version=latest
ENV VERSION ${version}
ADD target/service-broker-${VERSION}.jar /service-broker.jar
ENV JAVA_OPTS "-DLOG_LEVEL=info"

CMD ["/opt/run-java/launch_java.sh", "/service-broker.jar"]

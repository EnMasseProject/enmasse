FROM enmasseproject/java-base:8-8

ARG version=latest
ENV VERSION ${version}
ADD target/mqtt-gateway-${VERSION}.jar /mqtt-gateway.jar

CMD ["/opt/run-java/launch_java.sh", "/mqtt-gateway.jar"]

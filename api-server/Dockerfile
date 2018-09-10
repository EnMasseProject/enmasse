FROM enmasseproject/java-base:8-7

ARG version=latest
ENV VERSION ${version}
ADD target/api-server-${VERSION}.jar /api-server.jar

CMD ["/opt/run-java/launch_java.sh", "/api-server.jar"]

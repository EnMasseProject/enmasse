FROM enmasseproject/java-base:8-6

ARG version=latest
ENV VERSION ${version}
ADD target/api-server-${VERSION}.jar /api-server.jar
ENV JAVA_OPTS "-DLOG_LEVEL=info"

CMD ["/opt/run-java/launch_java.sh", "/api-server.jar"]

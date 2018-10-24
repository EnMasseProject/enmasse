FROM enmasseproject/java-base:8-9

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD target/api-server-${VERSION}.jar /api-server.jar

CMD ["/opt/run-java/launch_java.sh", "/api-server.jar"]

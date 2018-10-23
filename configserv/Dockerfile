FROM enmasseproject/java-base:8-9

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/configserv-${VERSION}.jar /configserv.jar

CMD ["/opt/run-java/launch_java.sh", "/configserv.jar"]

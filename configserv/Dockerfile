FROM enmasseproject/java-base:8-7

ARG version=latest
ENV VERSION ${version}
ADD target/configserv-${VERSION}.jar /configserv.jar

CMD ["/opt/run-java/launch_java.sh", "/configserv.jar"]

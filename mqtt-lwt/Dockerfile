FROM enmasseproject/java-base:8-6

ARG version=latest
ENV VERSION ${version}
ADD target/mqtt-lwt-${VERSION}.jar /mqtt-lwt.jar

CMD ["/opt/run-java/launch_java.sh", "/mqtt-lwt.jar"]

FROM enmasseproject/java-base:8-9

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/mqtt-lwt-${VERSION}.jar /mqtt-lwt.jar

CMD ["/opt/run-java/launch_java.sh", "/mqtt-lwt.jar"]

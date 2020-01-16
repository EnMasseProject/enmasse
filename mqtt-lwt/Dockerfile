FROM quay.io/enmasse/java-base:11-1

ARG version
ARG maven_version
ARG commit

ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/mqtt-lwt-${maven_version}.jar /mqtt-lwt.jar

CMD ["/opt/run-java/launch_java.sh", "/mqtt-lwt.jar"]

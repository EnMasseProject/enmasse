FROM enmasseproject/java-base:11-0

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/iot-auth-service-${MAVEN_VERSION}.jar /iot-auth-service.jar
ADD launch_java.sh /opt/run-java/
RUN chmod a+x /opt/run-java/launch_java.sh

CMD ["/opt/run-java/launch_java.sh", "/iot-auth-service.jar"]

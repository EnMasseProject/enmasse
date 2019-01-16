FROM enmasseproject/java-base:8-11

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/standard-controller-${MAVEN_VERSION}.jar /standard-controller.jar

CMD ["/opt/run-java/launch_java.sh", "/standard-controller.jar"]

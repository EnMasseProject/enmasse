FROM enmasseproject/java-base:11-0

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD target/keycloak-controller-${MAVEN_VERSION}.jar /keycloak-controller.jar


CMD ["/opt/run-java/launch_java.sh", "/keycloak-controller.jar"]

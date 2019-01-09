FROM enmasseproject/java-base:8-11

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/keycloak-controller-${VERSION}.jar /keycloak-controller.jar


CMD ["/opt/run-java/launch_java.sh", "/keycloak-controller.jar"]

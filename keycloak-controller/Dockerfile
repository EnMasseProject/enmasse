FROM enmasseproject/java-base:8-7

ARG version=latest
ENV VERSION ${version}
ADD target/keycloak-controller-${VERSION}.jar /keycloak-controller.jar


CMD ["/opt/run-java/launch_java.sh", "/keycloak-controller.jar"]

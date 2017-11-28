FROM enmasseproject/java-base:8-3

ARG version=latest
ENV VERSION ${version}
ADD target/keycloak-controller-${VERSION}.jar /keycloak-controller.jar

CMD ["/bin/launch_java.sh", "/keycloak-controller.jar"]

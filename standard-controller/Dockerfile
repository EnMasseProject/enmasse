FROM enmasseproject/java-base:8-3

ARG version=latest
ENV VERSION ${version}
ADD target/standard-controller-${VERSION}.jar /standard-controller.jar

CMD ["/bin/launch_java.sh", "/standard-controller.jar"]

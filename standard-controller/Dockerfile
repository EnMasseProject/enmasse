FROM enmasseproject/java-base:8-10

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/standard-controller-${VERSION}.jar /standard-controller.jar

CMD ["/opt/run-java/launch_java.sh", "/standard-controller.jar"]

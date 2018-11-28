FROM enmasseproject/java-base:8-10

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/topic-forwarder-${VERSION}.jar /topic-forwarder.jar

CMD ["/opt/run-java/launch_java.sh", "/topic-forwarder.jar"]

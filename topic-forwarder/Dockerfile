FROM enmasseproject/java-base:8-7

ARG version=latest
ENV VERSION ${version}
ADD target/topic-forwarder-${VERSION}.jar /topic-forwarder.jar

CMD ["/opt/run-java/launch_java.sh", "/topic-forwarder.jar"]

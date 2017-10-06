FROM enmasseproject/java-base:8-1

ARG version=latest
ENV VERSION ${version}
ADD target/topic-forwarder.jar /

CMD ["/bin/launch_java.sh", "/topic-forwarder.jar"]

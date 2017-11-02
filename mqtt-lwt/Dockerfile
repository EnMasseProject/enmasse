FROM enmasseproject/java-base:8-3

ARG version=latest
ENV VERSION ${version}
ADD target/mqtt-lwt.jar /

CMD ["/bin/launch_java.sh", "/mqtt-lwt.jar"]

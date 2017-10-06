FROM enmasseproject/java-base:8-1

ARG version=latest
ENV VERSION ${version}
ADD target/mqtt-lwt.jar /

CMD ["/bin/launch_java.sh", "/mqtt-lwt.jar"]

FROM enmasseproject/java-base:8-2

ARG version=latest
ENV VERSION ${version}
ADD target/queue-scheduler.jar /

CMD ["/bin/launch_java.sh", "/queue-scheduler.jar"]

FROM enmasseproject/java-base:8-1

ARG version=latest
ENV VERSION ${version}
ADD target/queue-scheduler.jar /

CMD ["/bin/launch_java.sh", "/queue-scheduler.jar"]

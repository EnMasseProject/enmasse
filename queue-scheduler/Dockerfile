FROM enmasseproject/java-base:8-3

ARG version=latest
ENV VERSION ${version}
ADD target/queue-scheduler-${VERSION}.jar /queue-scheduler.jar

CMD ["/bin/launch_java.sh", "/queue-scheduler.jar"]

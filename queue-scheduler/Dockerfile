FROM enmasseproject/java-base:8-7

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/queue-scheduler-${VERSION}.jar /queue-scheduler.jar

CMD ["/opt/run-java/launch_java.sh", "/queue-scheduler.jar"]

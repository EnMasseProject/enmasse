FROM enmasseproject/java-base:8-3

ARG version=latest
ENV VERSION ${version}
ADD target/configserv.jar /

CMD ["/bin/launch_java.sh", "/configserv.jar"]

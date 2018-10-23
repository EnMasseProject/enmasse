FROM enmasseproject/java-base:8-9

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD target/service-broker-${VERSION}.jar /service-broker.jar

CMD ["/opt/run-java/launch_java.sh", "/service-broker.jar"]

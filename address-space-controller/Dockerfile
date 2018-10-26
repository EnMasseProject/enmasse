FROM enmasseproject/java-base:8-9

RUN yum -y install openssl && yum -y clean all

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD target/address-space-controller-${VERSION}.jar /address-space-controller.jar
ADD target/classes/brokeredinfraconfigs /brokeredinfraconfigs
ADD target/classes/standardinfraconfigs /standardinfraconfigs
ADD target/classes/addressspaceplans /addressspaceplans
ADD target/classes/addressplans /addressplans

ENV LOG_LEVEL info

CMD ["/opt/run-java/launch_java.sh", "/address-space-controller.jar"]

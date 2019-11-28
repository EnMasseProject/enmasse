FROM quay.io/enmasse/java-base:11-1

ARG version
ARG commit
ARG maven_version
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/api-client.jar /
ADD target/probe-client.jar /
ADD target/messaging-client.jar /

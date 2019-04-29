FROM quay.io/enmasse/nodejs-base:10-1

RUN mkdir -p /opt/app-root/src/
WORKDIR /opt/app-root/src/

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/agent-${MAVEN_VERSION}-dist.tar.gz /opt/app-root/src/

EXPOSE 56720 8080

CMD ["/opt/app-root/src/bin/launch_node.sh", "/opt/app-root/src/bin/agent.js"]

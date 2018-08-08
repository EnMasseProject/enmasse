FROM enmasseproject/nodejs-base:6-1

RUN mkdir -p /opt/app-root/src/
WORKDIR /opt/app-root/src/

ARG version=latest
ENV VERSION ${version}
ADD target/agent-${VERSION}-dist.tar.gz /opt/app-root/src/

EXPOSE 56720 8080

CMD ["/opt/app-root/src/bin/launch_node.sh", "/opt/app-root/src/bin/agent.js"]

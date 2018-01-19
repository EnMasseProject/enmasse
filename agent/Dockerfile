FROM enmasseproject/nodejs-base:6

RUN mkdir -p /opt/app-root/src/
WORKDIR /opt/app-root/src/

ARG version=latest
ENV VERSION ${version}
ADD build/agent-${VERSION}.tgz /opt/app-root/src/

EXPOSE 56720 8080

CMD ["node", "/opt/app-root/src/bin/agent.js"]

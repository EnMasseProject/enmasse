FROM enmasseproject/nodejs-base:6-1

RUN mkdir -p /opt/app-root/src/
RUN cd /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install debug && npm install rhea"]
ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD build/none-authservice-${MAVEN_VERSION}.tgz /opt/app-root/src/
EXPOSE 5671

CMD ["node", "/opt/app-root/src/none-authservice.js"]

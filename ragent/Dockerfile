FROM enmasseproject/nodejs-base:6

RUN mkdir -p /opt/app-root/src/
RUN cd /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install debug && npm install rhea"]
ARG version=latest
ENV VERSION ${version}

ADD build/ragent-${VERSION}.tgz /opt/app-root/src/
EXPOSE 55672

CMD ["node", "/opt/app-root/src/ragent.js"]

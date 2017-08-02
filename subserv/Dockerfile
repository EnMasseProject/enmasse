FROM enmasseproject/nodejs-base:6

RUN mkdir -p /opt/app-root/src/
RUN cd /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install debug && npm install bluebird && npm install rhea"]
ARG version=latest

ADD build/subserv.tgz /opt/app-root/src/
EXPOSE 5672

CMD ["node", "/opt/app-root/src/subserv.js"]

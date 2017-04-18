FROM centos:7

RUN yum -y update  \
    && yum -y install epel-release \
    && yum -y install nodejs \
    npm \
    && yum clean all

RUN mkdir -p /opt/app-root/src/
RUN cd /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install debug && npm install bluebird && npm install rhea"]
ARG version=latest

ADD subserv-${version}.tar.z /opt/app-root/src/
EXPOSE 5672

CMD ["node", "/opt/app-root/src/subserv.js"]

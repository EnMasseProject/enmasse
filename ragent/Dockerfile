FROM centos:7

RUN yum -y update  \
    && yum -y install epel-release \
    && yum -y install nodejs \
    npm \
    && yum clean all

RUN mkdir -p /opt/app-root/src/
RUN cd /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install debug && npm install rhea"]
ARG version=latest

ADD build/ragent.tgz /opt/app-root/src/
EXPOSE 55672

CMD ["node", "/opt/app-root/src/ragent.js"]

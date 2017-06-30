FROM centos:7

RUN yum -y update  \
    && yum -y install epel-release cyrus-sasl-lib \
    && yum -y install nodejs npm \
    && yum clean all

RUN mkdir -p /opt/app-root/src/
WORKDIR /opt/app-root/src/

ARG version=latest
ADD console-${version}.tar.gz /opt/app-root/src/

RUN ["/bin/bash", "-c", "npm install"]

EXPOSE 56720 8080

CMD ["node", "/opt/app-root/src/bin/console_server.js"]

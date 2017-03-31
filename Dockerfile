FROM centos:7

RUN yum -y update  \
    && yum -y install epel-release cyrus-sasl-lib \
    && yum -y install nodejs npm \
    && yum clean all

RUN mkdir -p /opt/app-root/src/
WORKDIR /opt/app-root/src/

COPY package.json /opt/app-root/src/
RUN ["/bin/bash", "-c", "npm install"]

COPY . /opt/app-root/src/
EXPOSE 56720 8080

CMD ["node", "/opt/app-root/src/bin/console_server.js"]

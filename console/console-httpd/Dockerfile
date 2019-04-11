FROM centos:7

RUN yum -y install httpd mod_ssl mod_proxy && yum -y clean all

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/console-httpd-${MAVEN_VERSION}-dist.tar.gz /

EXPOSE 8080
EXPOSE 9443

ENTRYPOINT apachectl -DFOREGROUND

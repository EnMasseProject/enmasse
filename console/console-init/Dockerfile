FROM centos:7
RUN yum -y install gettext && yum -y clean all

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/console-init-${MAVEN_VERSION}-dist.tar.gz /

CMD ["/oauth-proxy/bin/init.sh", "/apps/"]

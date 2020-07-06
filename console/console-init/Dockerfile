FROM centos:8
RUN dnf install -y gettext python3 findutils tar && dnf clean all -y

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD target/console-init-${maven_version}-dist.tar.gz /

CMD ["/oauth-proxy/bin/init.sh", "/apps/"]

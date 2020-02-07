FROM registry.fedoraproject.org/fedora-minimal:31
RUN microdnf install gettext && microdnf clean all

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/console-init-${maven_version}-dist.tar.gz /

CMD ["/oauth-proxy/bin/init.sh", "/apps/"]

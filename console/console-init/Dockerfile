FROM quay.io/enmasse/fedora-minimal:33
RUN microdnf install gettext python findutils coreutils tar && microdnf clean all

ARG version
ARG maven_version
ARG revision
ENV VERSION=${version} REVISION=${revision} MAVEN_VERSION=${maven_version}

ADD target/console-init-${maven_version}-dist.tar.gz /

CMD ["/oauth-proxy/bin/init.sh", "/apps/"]

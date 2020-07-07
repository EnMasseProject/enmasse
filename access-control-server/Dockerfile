#
# Copyright 2018-2020, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM scratch

ARG version
ARG revision
ARG maven_version
ENV VERSION=${version} MAVEN_VERSION=${maven_version} REVISION=${revision}

ADD target/access-control-server-${maven_version}-dist.tar.gz /

ENTRYPOINT ["/access-control-server"]

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM enmasseproject/qdrouterd-base:1.5-rc2

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} MAVEN_VERSION=${maven_version} COMMIT=${commit}

ADD build/qdr-proxy-configurator /

ENTRYPOINT /qdr-proxy-configurator

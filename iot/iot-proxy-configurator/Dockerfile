#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM quay.io/enmasse/qdrouterd-base:1.8.0

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} MAVEN_VERSION=${maven_version} COMMIT=${commit}

ADD target/iot-proxy-configurator-${MAVEN_VERSION}-dist.tar.gz /

ENTRYPOINT /iot-proxy-configurator

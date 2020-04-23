#
# Copyright 2018-2019, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM quay.io/enmasse/fedora-minimal:31

ARG version
ARG commit
ARG maven_version
ENV VERSION=${version} MAVEN_VERSION=${maven_version} COMMIT=${commit}

ADD target/olm-manifest-${maven_version}-dist.tar.gz /
RUN /bin/replace-images.sh
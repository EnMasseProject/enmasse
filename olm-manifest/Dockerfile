#
# Copyright 2018-2019, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM quay.io/openshift/origin-operator-registry:latest

ARG version
ARG commit
ARG maven_version
ENV VERSION=${version} MAVEN_VERSION=${maven_version} COMMIT=${commit}

ADD target/olm-manifest-${maven_version}-dist.tar.gz /
USER root
RUN /bin/replace-images.sh
USER 1001

RUN /usr/bin/initializer -m /manifests -o bundles.db
ENTRYPOINT ["/usr/bin/registry-server"]
CMD ["-d", "bundles.db", "-t", "termination.log"]

#
# Copyright 2018-2019, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM centos:7

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD build/operatorImageMap.yaml /etc/operatorImageMap.yaml
ADD build/enmasse-controller-manager /

ENTRYPOINT /enmasse-controller-manager

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

FROM enmasseproject/qdrouterd-base:1.4.1

ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD iot-gc /

ENTRYPOINT /iot-gc

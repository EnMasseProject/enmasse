#
# Copyright 2018-2019, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#
FROM quay.io/operator-framework/upstream-registry-builder as builder

ARG version
ARG commit
ARG maven_version

ADD target/olm-manifest-${maven_version}-dist.tar.gz /

RUN find /manifests
RUN find /metadata

RUN /bin/initializer -o ./bundles.db

FROM scratch

ENV VERSION=${version} MAVEN_VERSION=${maven_version} COMMIT=${commit}
COPY --from=builder /build/bundles.db /bundles.db
COPY --from=builder /bin/registry-server /registry-server
COPY --from=builder /bin/grpc_health_probe /bin/grpc_health_probe
EXPOSE 50051
ENTRYPOINT ["/registry-server"]
CMD ["--database", "bundles.db"]

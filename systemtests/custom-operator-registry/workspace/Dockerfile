ARG MANIFESTS_IMAGE
FROM ${MANIFESTS_IMAGE} AS manifests_image

FROM quay.io/operator-framework/upstream-registry-builder:latest

COPY --from=manifests_image /manifests manifests

#TODO replace manifests_replacer.sh contents in CI pipelines with actual replacement logic needed
ADD manifests_replacer.sh manifests_replacer.sh
RUN chmod +x /workspace/manifests_replacer.sh && bash /workspace/manifests_replacer.sh

RUN /bin/initializer -o ./bundles.db
EXPOSE 50051
ENTRYPOINT ["/bin/registry-server"]
CMD ["--database", "/build/bundles.db"]
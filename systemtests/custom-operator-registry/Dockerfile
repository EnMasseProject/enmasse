FROM quay.io/operator-framework/upstream-registry-builder:latest

COPY manifests manifests
RUN /bin/initializer -o ./bundles.db

EXPOSE 50051
ENTRYPOINT ["/bin/registry-server"]
CMD ["--database", "/build/bundles.db"]
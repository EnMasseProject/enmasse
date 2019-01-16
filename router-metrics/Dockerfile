FROM gordons/qpid-proton:0.18
RUN pip install prometheus_client
ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}
ADD build/router-metrics-${MAVEN_VERSION}.tgz /

EXPOSE 8080
CMD ["python", "-u", "/router-metrics.py"]

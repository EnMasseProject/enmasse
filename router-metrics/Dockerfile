FROM gordons/qpid-proton:0.18
RUN pip install prometheus_client
ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}
ADD build/router-metrics-${VERSION}.tgz /

EXPOSE 8080
CMD ["python", "-u", "/router-metrics.py"]

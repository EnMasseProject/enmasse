FROM ppatierno/qpid-proton:0.17.0
RUN pip install prometheus_client
ARG version=latest
ENV VERSION=${version}
ADD router-metrics-${VERSION}.tgz /

EXPOSE 8080
CMD ["python", "/router-metrics.py"]

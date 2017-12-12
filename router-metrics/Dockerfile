FROM gordons/qpid-proton:0.18
RUN pip install prometheus_client
ARG version=latest
ENV VERSION=${version}
ADD router-metrics-${VERSION}.tgz /

EXPOSE 8080
CMD ["python", "/router-metrics.py"]

FROM ppatierno/qpid-proton:0.17.0
ADD router-metrics.py /
RUN pip install prometheus_client
ARG version=latest
ENV VERSION=${version}

EXPOSE 8080
CMD ["python", "/router-metrics.py"]

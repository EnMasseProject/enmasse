FROM quay.io/enmasse/qdrouterd-base:1.8.0
ARG version
ARG maven_version
ARG commit

ENV VERSION=${version} COMMIT=${commit} TZ=GMT0 MAVEN_VERSION=${maven_version}

RUN dnf install -y gdb procps-ng
ADD build/router-${MAVEN_VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

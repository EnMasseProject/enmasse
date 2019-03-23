FROM enmasseproject/qdrouterd-base:2a4e7ecc
ARG version
ARG maven_version
ARG commit

ENV VERSION=${version} COMMIT=${commit} TZ=GMT0 MAVEN_VERSION=${maven_version}

ADD build/router-${MAVEN_VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

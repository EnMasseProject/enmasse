FROM enmasseproject/qdrouterd-base:1.5-DISPATCH-1273-1287
ARG version
ARG maven_version
ARG commit

ENV VERSION=${version} COMMIT=${commit} TZ=GMT0 MAVEN_VERSION=${maven_version}

ADD build/router-${MAVEN_VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

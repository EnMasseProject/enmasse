FROM enmasseproject/qdrouterd-base:1.4.1
ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

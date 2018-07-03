FROM enmasseproject/qdrouterd-base:1.2.0
ARG version=latest
ENV VERSION=${version}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

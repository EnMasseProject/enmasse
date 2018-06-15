FROM enmasseproject/qdrouterd-base:1.1.0_proton-0.23.0-http-DISPATCH-1034-1039
ARG version=latest
ENV VERSION=${version}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

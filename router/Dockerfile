FROM enmasseproject/qdrouterd-base:master-2018-03-14
ARG version=latest
ENV VERSION=${version}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/ 

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

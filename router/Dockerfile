FROM enmasseproject/qdrouterd-base:1.3.0-DISPATCH-1111-DISPATCH-1116-DISPATCH-1086-proton-0.25.0
ARG version=latest
ENV VERSION=${version}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

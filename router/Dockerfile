FROM gordons/qdrouterd-base:debug2
ARG version
ARG commit
ENV VERSION=${version} COMMIT=${commit}

ADD build/router-${VERSION}.tgz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

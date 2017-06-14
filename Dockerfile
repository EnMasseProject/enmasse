FROM ppatierno/qpid-proton:0.17.0
ADD qpid-dispatch-image.tar.gz /
RUN dnf -y install gettext hostname iputils
ARG version=latest
ENV VERSION=${version}

EXPOSE 5672 55672 5671
CMD ["/sbin/qdrouterd", "-c", "/etc/qpid-dispatch/qdrouterd.conf"]

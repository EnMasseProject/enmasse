FROM ppatierno/qpid-proton:0.17.0
RUN dnf -y install gettext hostname iputils
ADD qpid-dispatch-image.tar.gz /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

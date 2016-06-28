FROM gordons/qpid-proton:0.12.2
ADD qpid-dispatch-binary.tar.gz /
RUN dnf -y install gettext hostname
COPY ./run_qdr.sh ./qdrouterd.conf.template colocated.snippet ssl.snippet /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

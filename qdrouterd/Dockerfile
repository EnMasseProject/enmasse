FROM gordons/qpid-proton:0.13.1
ADD qpid-dispatch-binary.tar.gz /
RUN dnf -y install gettext hostname iputils
COPY ./run_qdr.sh ./qdrouterd.conf.template noncolocated.snippet colocated-topic.snippet ssl.snippet subscriptions.snippet /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

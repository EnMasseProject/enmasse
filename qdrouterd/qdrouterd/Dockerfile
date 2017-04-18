FROM ppatierno/qpid-proton:0.17.0
ADD qpid-dispatch-binary.tar.gz /
RUN dnf -y install gettext hostname iputils
COPY ./run_qdr.sh ./qdrouterd.conf.template colocated-topic.snippet ssl.snippet subscriptions.snippet amqp-kafka-bridge.snippet /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

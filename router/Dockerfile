FROM gordons/qdrouterd-base:DISPATCH-928-v2
ARG version=latest

RUN dnf -y install valgrind
COPY ./run_qdr.sh ./qdrouterd.conf.template colocated-topic.snippet subscriptions.snippet amqp-kafka-bridge.snippet /etc/qpid-dispatch/

ENV VERSION=${version}

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

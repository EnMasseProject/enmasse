FROM enmasseproject/qdrouterd-base:0.9.0-1
ARG version=latest

COPY ./run_qdr.sh ./qdrouterd.conf.template colocated-topic.snippet ssl.snippet subscriptions.snippet amqp-kafka-bridge.snippet /etc/qpid-dispatch/

ENV VERSION=${version}

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

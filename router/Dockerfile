FROM gordons/qdrouter-base:DISPATCH-920
ARG version=latest

COPY ./run_qdr.sh ./qdrouterd.conf.template colocated-topic.snippet subscriptions.snippet amqp-kafka-bridge.snippet policy.json /etc/qpid-dispatch/

ENV VERSION=${version}

EXPOSE 5672 55672 5671
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

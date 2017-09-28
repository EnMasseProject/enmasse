FROM enmasseproject/java-base:8

ARG version=latest
ENV VERSION ${version}
ADD build/distributions/queue-scheduler.tgz /

EXPOSE 55667

CMD /queue-scheduler/bin/queue-scheduler

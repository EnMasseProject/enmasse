FROM openjdk:8-jre-alpine

ARG version=latest
ENV VERSION ${version}
RUN apk add --no-cache bash
ADD build/distributions/queue-scheduler-${version}.tar /

EXPOSE 55667

CMD /queue-scheduler-${VERSION}/bin/queue-scheduler

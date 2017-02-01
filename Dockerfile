FROM openjdk:8-jre-alpine

RUN apk add --no-cache bash
ADD build/distributions/queue-scheduler.tar /

EXPOSE 55667

CMD ["/queue-scheduler/bin/queue-scheduler"]

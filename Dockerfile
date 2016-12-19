FROM openjdk:8-jre-alpine

RUN apk add --no-cache bash
ADD build/distributions/topic-forwarder.tar /

EXPOSE 8080

CMD ["/topic-forwarder/bin/topic-forwarder"]

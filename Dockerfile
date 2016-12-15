FROM openjdk:8-jre-alpine

RUN apk add --no-cache bash
ADD build/distributions/configserv.tar /

EXPOSE 5672

CMD ["/configserv/bin/configserv"]

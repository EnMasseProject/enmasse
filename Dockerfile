FROM openjdk:8-jre-alpine

ARG version=latest
ENV VERSION ${version}
RUN apk add --no-cache bash
ADD build/distributions/topic-forwarder-${version}.tar /

EXPOSE 8080

CMD /topic-forwarder-${VERSION}/bin/topic-forwarder

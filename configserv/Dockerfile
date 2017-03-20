FROM openjdk:8-jre-alpine

ARG version=latest
ENV VERSION ${version}
RUN apk add --no-cache bash
ADD build/distributions/configserv-${version}.tar /

EXPOSE 5672

CMD /configserv-${VERSION}/bin/configserv

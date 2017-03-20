FROM openjdk:8-jre-alpine

ARG version=latest
ENV VERSION ${version}
RUN apk add --no-cache bash
ADD build/distributions/server-${version}.tar /

EXPOSE 5672
EXPOSE 8080

CMD /server-${VERSION}/bin/server

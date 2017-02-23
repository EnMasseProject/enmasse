FROM openjdk:8-jre-alpine

ARG version=latest
ENV VERSION ${version}
RUN apk add --no-cache bash
ADD build/distributions/address-controller-server-${version}.tar /

EXPOSE 55674
EXPOSE 8080

CMD /address-controller-server-${VERSION}/bin/address-controller-server

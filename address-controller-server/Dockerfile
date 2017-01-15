FROM openjdk:8-jre-alpine

RUN apk add --no-cache bash
ADD build/distributions/address-controller-server.tar /

EXPOSE 55674
EXPOSE 8080

CMD ["/address-controller-server/bin/address-controller-server"]

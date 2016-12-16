FROM openjdk:8-jre-alpine

RUN apk add --no-cache bash
ADD build/distributions/storage-controller-server.tar /

EXPOSE 55674

CMD ["/storage-controller-server/bin/storage-controller-server"]

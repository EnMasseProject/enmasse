FROM openjdk:8-jre

ADD target/mqtt-frontend-1.0-SNAPSHOT.jar /

EXPOSE 1883

CMD ["java", "-jar", "/mqtt-frontend-1.0-SNAPSHOT.jar"]
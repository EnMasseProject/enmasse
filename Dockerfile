FROM openjdk:8-jre

ADD target/mqtt-frontend-1.0-SNAPSHOT.jar /

EXPOSE 1883

CMD ["java", "-Dvertx.disableFileCaching=true", "-Dvertx.disableFileCPResolving=true", "-jar", "/mqtt-frontend-1.0-SNAPSHOT.jar" ]
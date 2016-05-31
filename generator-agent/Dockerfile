FROM java:8

ADD target/config-generator-agent-1.0-SNAPSHOT-jar-with-dependencies.jar /config-generator-agent.jar
ADD src/main/sh/config-generator-agent.sh /config-generator-agent

CMD ["/config-generator-agent"]

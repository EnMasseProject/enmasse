FROM java:8

ADD target/config-generator-agent-1.0-SNAPSHOT-jar-with-dependencies.jar /config-generator-agent.jar
ADD src/main/sh/config-generator-agent.sh /config-generator-agent

ENV OPENSHIFT_USER test
ENV OPENSHIFT_TOKEN aBKwDeeQR9ZZcnmq7iv-3hk4k-7hAwWIfZRr5u0zVTg
ENV OPENSHIFT_NAMESPACE petproject

CMD ["/config-generator-agent"]

FROM jboss/keycloak:3.2.1.Final

ARG version=latest

ADD ./sasl-plugin/build/libs/sasl-plugin-all.jar /opt/jboss/keycloak/providers
ADD ./src/main/xsl/addSaslPlugin.xsl /opt/jboss/keycloak/

RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/addSaslPlugin.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml


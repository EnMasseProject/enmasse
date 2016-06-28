FROM tomcat:jre8
EXPOSE 8080
RUN chmod -R ugo+rw /usr/local/tomcat/
COPY hawtio.war /usr/local/tomcat/webapps/
COPY dispatch-plugin.war /usr/local/tomcat/webapps/

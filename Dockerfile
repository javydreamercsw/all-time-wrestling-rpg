FROM tomcat:11-jdk17
COPY src/main/resources/docker/tomcat/server.xml /usr/local/tomcat/conf/server.xml
COPY target/all-time-wrestling-rpg-*.war /usr/local/tomcat/webapps/atw-rpg.war
EXPOSE 9090

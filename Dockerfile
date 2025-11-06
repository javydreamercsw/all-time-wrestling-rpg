FROM tomcat:11-jdk17
COPY target/all-time-wrestling-rpg-*.war /usr/local/tomcat/webapps/atw-rpg.war
EXPOSE 9090

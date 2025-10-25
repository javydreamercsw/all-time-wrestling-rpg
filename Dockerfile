FROM tomcat:11-jdk17
COPY target/all-time-wrestling-rpg-1.0.0-SNAPSHOT.war /usr/local/tomcat/webapps/all-time-wrestling-rpg.war
EXPOSE 9090

FROM tomcat:11-jdk17
COPY src/main/resources/docker/tomcat/server.xml /usr/local/tomcat/conf/server.xml
COPY target/all-time-wrestling-rpg-*.war /usr/local/tomcat/webapps/atw-rpg.war
ENV GEMINI_API_KEY=
ENV OPENAI_API_KEY=
ENV NOTION_TOKEN=
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/data/atwrpg;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=
ENV SPRING_H2_CONSOLE_ENABLED=true
VOLUME /data
EXPOSE 9090

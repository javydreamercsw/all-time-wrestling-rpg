FROM tomcat:11-jdk25
COPY src/main/resources/docker/tomcat/server.xml /usr/local/tomcat/conf/server.xml
RUN keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
    -storetype JKS -keystore /usr/local/tomcat/conf/keystore.jks \
    -validity 36500 -storepass changeit -keypass changeit \
    -dname "CN=localhost, OU=Test, O=Test, L=Test, S=Test, C=US"
COPY target/all-time-wrestling-rpg-*.war /usr/local/tomcat/webapps/atw-rpg.war

# Default non-sensitive configurations
ENV AI_TIMEOUT=300
ENV AI_PROVIDER_AUTO=true
ENV AI_OPENAI_ENABLED=false
ENV AI_OPENAI_API_URL=https://api.openai.com/v1/chat/completions
ENV AI_OPENAI_DEFAULT_MODEL=gpt-3.5-turbo
ENV AI_OPENAI_PREMIUM_MODEL=gpt-4
ENV AI_OPENAI_MAX_TOKENS=1000
ENV AI_OPENAI_TEMPERATURE=0.7
ENV AI_CLAUDE_ENABLED=false
ENV AI_CLAUDE_API_URL=https://api.anthropic.com/v1/messages/
ENV AI_CLAUDE_MODEL_NAME=claude-3-haiku-20240307
ENV AI_GEMINI_ENABLED=false
ENV AI_GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/
ENV AI_GEMINI_MODEL_NAME=gemini-2.5-flash

# Note: AI_OPENAI_API_KEY, AI_CLAUDE_API_KEY, AI_GEMINI_API_KEY, and NOTION_TOKEN 
# must be provided at runtime for AI/Notion features to work.

ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/data/atwrpg;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_H2_CONSOLE_ENABLED=true

# Note: SPRING_DATASOURCE_PASSWORD must be provided at runtime if required by the database.

VOLUME /data
EXPOSE 9090
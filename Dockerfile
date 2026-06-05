# Build stage: download Maven 3.9.14 and build the production JAR (Jetty embedded)
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends curl git && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.tar.gz \
    | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.14/bin/mvn /usr/local/bin/mvn
WORKDIR /app
RUN git init
# Cache dependency downloads as a separate layer — only re-runs when pom.xml changes
COPY pom.xml ./
RUN mvn dependency:go-offline -Pproduction,docker -B -q
COPY . .
RUN mvn -Pproduction,docker package -DskipTests -B

# Runtime stage: slim JRE — no standalone Tomcat
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/all-time-wrestling-rpg-*.jar app.jar

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

ENV SPRING_PROFILES_ACTIVE=prod,mysql

# Note: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
# must be provided at runtime via environment variables.

# Railway injects PORT; default to 8080 for local docker run
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

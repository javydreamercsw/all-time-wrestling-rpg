FROM eclipse-temurin:21-jre
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.config.location=file:/config/application.properties"]

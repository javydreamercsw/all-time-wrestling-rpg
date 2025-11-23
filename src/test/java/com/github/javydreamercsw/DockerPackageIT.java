package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * This is an integration test that packages the application into a Docker image and runs it. It
 * must be run with the 'docker-it' profile to ensure the .war file is built before the test. e.g.
 * `mvn verify -Pdocker-it`
 */
class DockerPackageIT {

  @Test
  void testDockerPackage() throws Exception {
    // Build the image from the Dockerfile
    ImageFromDockerfile image =
        new ImageFromDockerfile()
            .withDockerfile(new File("./Dockerfile").getAbsoluteFile().toPath());
    final int port = Integer.parseInt(System.getProperty("server.port", "9090"));
    final String contextPath = System.getProperty("server.servlet.context-path", "/atw-rpg");

    try (GenericContainer<?> container =
        new GenericContainer<>(image)
            .withExposedPorts(port)
            .withEnv("notion.sync.enabled", "false")
            .withEnv("notion.sync.scheduler.enabled", "false")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:h2:mem:testdb")
            .withEnv(
                "JAVA_TOOL_OPTIONS", "-Dspring.profiles.active=prod -Dvaadin.productionMode=true")
            .waitingFor(
                Wait.forHttp(contextPath + "/actuator/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(1)))) {
      container.start();

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      "http://"
                          + container.getHost()
                          + ":"
                          + container.getMappedPort(port)
                          + contextPath
                          + "/actuator/health"))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      assertEquals(200, response.statusCode());
    }
  }
}

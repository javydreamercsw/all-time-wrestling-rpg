/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
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
            .withEnv("VAADIN_SERVLET_PARAMETER_PRODUCTION_MODE", "true")
            .withEnv("vaadin.devmode.enable", "false")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:h2:mem:testdb")
            .withEnv(
                "JAVA_TOOL_OPTIONS", "-Dspring.profiles.active=prod -Dvaadin.productionMode=true")
            .waitingFor(
                Wait.forHttps(contextPath + "/actuator/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(1)))) {
      container.start();

      HttpClient client =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      "https://"
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

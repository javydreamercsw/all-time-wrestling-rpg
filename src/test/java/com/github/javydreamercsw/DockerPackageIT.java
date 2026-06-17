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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * This is an integration test that packages the application into a Docker image and runs it. It
 * must be run with the 'docker-it' profile to ensure the .war file is built before the test. e.g.
 * `mvn verify -Pdocker-it`
 */
class DockerPackageIT {

  @Test
  void testDockerPackage() throws Exception {
    File projectDir = new File(".").getAbsoluteFile().getCanonicalFile();
    final int port = Integer.parseInt(System.getProperty("server.port", "8080"));
    final String contextPath = System.getProperty("server.servlet.context-path", "");

    // Build image via docker CLI with --pull=never so it uses the local base image without
    // hitting Docker Hub. This avoids the Testcontainers pre-pull hang on ImageFromDockerfile.
    String imageTag = "atw-rpg-it:latest";
    Process build =
        new ProcessBuilder("docker", "build", "-t", imageTag, projectDir.getAbsolutePath())
            .directory(projectDir)
            .redirectErrorStream(true)
            .start();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(build.getInputStream()))) {
      reader.lines().forEach(line -> System.out.println("[docker build] " + line));
    }
    assertEquals(0, build.waitFor(), "docker build failed");

    try (GenericContainer<?> container =
        new GenericContainer<>(DockerImageName.parse(imageTag))
            .withExposedPorts(port)
            .withEnv("DATA_INITIALIZER_ENABLED", "false")
            .withEnv("VAADIN_SERVLET_PARAMETER_PRODUCTION_MODE", "true")
            .withEnv("vaadin.devmode.enable", "false")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:h2:mem:testdb")
            .withEnv(
                "JAVA_TOOL_OPTIONS",
                """
                -Dspring.profiles.active=prod,h2 -Dvaadin.productionMode=true\
                 -Dhttps.enforcement.disabled=true\
                """)
            .waitingFor(
                Wait.forHttp(contextPath + "/actuator/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5)))) {
      container.start();

      HttpClient client =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
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

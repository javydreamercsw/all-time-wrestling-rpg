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
package com.github.javydreamercsw.base.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.config.LocalAIContainerConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {
      LocalAISegmentNarrationService.class,
      LocalAIContainerConfig.class,
      AiBaseProperties.class,
      LocalAIConfigProperties.class,
      LocalAIStatusService.class
    })
@EnableConfigurationProperties(SegmentNarrationConfig.class)
@ActiveProfiles("localai")
class LocalAISegmentNarrationServiceIT {

  @Autowired private LocalAISegmentNarrationService localAIService;
  @Autowired private LocalAIConfigProperties config;

  @Test
  void testGenerateText() {
    // Wait for LocalAI to be ready
    waitForLocalAI();

    // Given
    String prompt = "Who is the best wrestler of all time?";

    // When
    String response = localAIService.generateText(prompt);

    // Then
    assertNotNull(response);
    assertFalse(response.isEmpty());
    System.out.println("LocalAI Response: " + response);
  }

  private void waitForLocalAI() {
    long startTime = System.currentTimeMillis();
    long timeout = Duration.ofMinutes(15).toMillis(); // 15 minutes timeout
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(config.getBaseUrl() + "/readyz")).build();

      while (System.currentTimeMillis() - startTime < timeout) {
        try {
          HttpResponse<String> response =
              client.send(request, HttpResponse.BodyHandlers.ofString());
          if (response.statusCode() == 200) {
            System.out.println("LocalAI is ready!");
            return;
          }
        } catch (IOException | InterruptedException e) {
          // Ignore and retry
        }
        try {
          Thread.sleep(1000); // Check every second
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          fail("Interrupted while waiting for LocalAI");
        }
      }
      fail("Timeout waiting for LocalAI to be ready.");
    }
  }
}

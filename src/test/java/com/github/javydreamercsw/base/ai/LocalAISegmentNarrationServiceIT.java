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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.config.LocalAIContainerConfig;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(
    classes = {
      LocalAISegmentNarrationService.class,
      LocalAIContainerConfig.class,
      AiBaseProperties.class,
      LocalAIConfigProperties.class,
      LocalAIStatusService.class,
      AiSettingsService.class
    })
@EnableConfigurationProperties(SegmentNarrationConfig.class)
@Slf4j
@ActiveProfiles("local-ai-it")
class LocalAISegmentNarrationServiceIT {

  @Autowired private LocalAISegmentNarrationService localAIService;
  @Autowired private AiSettingsService aiSettingsService;

  @MockitoBean private GameSettingService gameSettingService;

  @Autowired private LocalAIContainerConfig containerConfig;
  @Autowired private LocalAIStatusService statusService;

  @BeforeEach
  void setUp() {
    // Mock the settings to return what we expect from the DB (gpt-4)
    GameSetting enabledSetting = new GameSetting();
    enabledSetting.setId("AI_LOCALAI_ENABLED");
    enabledSetting.setValue("true");

    GameSetting modelSetting = new GameSetting();
    modelSetting.setId("AI_LOCALAI_MODEL");
    modelSetting.setValue("gpt-oss-120b");

    when(gameSettingService.findById("AI_LOCALAI_ENABLED"))
        .thenReturn(java.util.Optional.of(enabledSetting));
    when(gameSettingService.findById("AI_LOCALAI_MODEL"))
        .thenReturn(java.util.Optional.of(modelSetting));

    containerConfig.startLocalAiContainer(true);
    // Wait for the container to be ready
    long startTime = System.currentTimeMillis();
    long timeout = Duration.ofMinutes(30).toMillis(); // Match container startup timeout

    log.info("Starting Container...");
    while (statusService.getStatus() != LocalAIStatusService.Status.READY) {
      if (System.currentTimeMillis() - startTime > timeout) {
        fail(
            "Timeout waiting for LocalAI container to be ready. Status: "
                + statusService.getStatus()
                + ", Message: "
                + statusService.getMessage());
      }
      if (statusService.getStatus() == LocalAIStatusService.Status.FAILED) {
        fail("LocalAI container failed to start. Message: " + statusService.getMessage());
      }
      try {
        Thread.sleep(1_000); // Check every second
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Interrupted while waiting for LocalAI container");
      }
    }
    GenericContainer<?> localAiContainer = containerConfig.getLocalAiContainer();
    assertNotNull(localAiContainer, "Container from config should not be null");
    assertTrue(localAiContainer.isRunning(), "Container should be running");

    String baseUrl =
        String.format(
            "http://%s:%d", localAiContainer.getHost(), localAiContainer.getMappedPort(8080));

    GameSetting baseUrlSetting = new GameSetting();
    baseUrlSetting.setId("AI_LOCALAI_BASE_URL");
    baseUrlSetting.setValue(baseUrl);
    when(gameSettingService.findById("AI_LOCALAI_BASE_URL"))
        .thenReturn(java.util.Optional.of(baseUrlSetting));
  }

  @Test
  void testGenerateText() {
    // Given
    String prompt = "Who is the best wrestler of all time?";

    // When
    String response = localAIService.generateText(prompt);

    // Then
    assertNotNull(response);
    assertFalse(response.isEmpty());
    System.out.println("LocalAI Response: " + response);
  }
}

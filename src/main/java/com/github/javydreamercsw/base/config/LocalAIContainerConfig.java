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
package com.github.javydreamercsw.base.config;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import java.io.File;
import java.time.Duration;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Manages a LocalAI container for development using Testcontainers. This configuration is only
 * active when the non-test profile is enabled.
 */
@Configuration
@Profile("localai")
@Slf4j
@RequiredArgsConstructor
public class LocalAIContainerConfig {

  @Value("${segment-narration.ai.localai.model}")
  private String modelName;

  private final LocalAIStatusService statusService;
  private GenericContainer<?> localAiContainer;

  @EventListener(ApplicationReadyEvent.class)
  public void startLocalAiContainer() {
    if (modelName != null && !modelName.isEmpty()) {
      new Thread(this::initializeAndStartContainer).start();
    }
  }

  private void initializeAndStartContainer() {
    try {
      statusService.setStatus(LocalAIStatusService.Status.STARTING);
      statusService.setMessage("LocalAI container is starting...");

      File modelsDir = new File("models");
      if (!modelsDir.exists()) {
        modelsDir.mkdirs();
      }

      // Using the official LocalAI image
      localAiContainer =
          new GenericContainer<>("localai/localai:latest")
              .withExposedPorts(8080)
              .withFileSystemBind(modelsDir.getAbsolutePath(), "/build/models", BindMode.READ_WRITE)
              .withCommand("run", modelName)
              .withEnv("MODELS_PATH", "/build/models")
              .waitingFor(
                  Wait.forHttp("/readyz")
                      .forStatusCode(200)
                      .withStartupTimeout(Duration.ofMinutes(15)));

      log.info(
          "Starting LocalAI container. This may take a while for the initial model download...");
      statusService.setStatus(LocalAIStatusService.Status.DOWNLOADING_MODEL);
      statusService.setMessage("Downloading/installing AI model. This can take several minutes...");

      localAiContainer.start();

      String baseUrl =
          String.format(
              "http://%s:%d", localAiContainer.getHost(), localAiContainer.getMappedPort(8080));
      System.setProperty("segment-narration.ai.localai.base-url", baseUrl);
      System.setProperty("segment-narration.ai.localai.model", modelName);

      statusService.setStatus(LocalAIStatusService.Status.READY);
      statusService.setMessage("LocalAI is ready.");
      log.info("LocalAI Container started at: {}", baseUrl);
      log.info("Model '{}' is ready.", modelName);

    } catch (Exception e) {
      log.error("Failed to start LocalAI container", e);
      statusService.setStatus(LocalAIStatusService.Status.FAILED);
      statusService.setMessage("LocalAI failed to start: " + e.getMessage());
    }
  }

  @PreDestroy
  public void stopLocalAiContainer() {
    if (localAiContainer != null) {
      localAiContainer.stop();
    }
  }
}

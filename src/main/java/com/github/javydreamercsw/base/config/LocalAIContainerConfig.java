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

import java.io.File;
import java.time.Duration;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Manages a LocalAI container for development using Testcontainers. This configuration is only
 * active when the "local-ai-embedded" profile is enabled.
 */
@Configuration
@Profile("!test & !e2e")
@Slf4j
public class LocalAIContainerConfig {

  @Value("${segment-narration.ai.localai.model}")
  private String modelName;

  private GenericContainer<?> localAiContainer;

  @EventListener(ApplicationReadyEvent.class)
  public void startLocalAiContainer() {
    File modelsDir = new File("models");
    if (!modelsDir.exists()) {
      modelsDir.mkdirs();
    }

    // Using the official LocalAI image
    localAiContainer =
        new GenericContainer<>("localai/localai:latest")
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath(modelsDir.toPath()), "/build/models")
            // Install the model on startup
            .withCommand("run", modelName)
            .withEnv("MODELS_PATH", "/build/models")
            // Wait for the server to be ready. Increased timeout for model download/install.
            .waitingFor(
                Wait.forHttp("/readyz")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(15)));

    localAiContainer.start();

    // Dynamically set the base URL for the LocalAI service
    String baseUrl =
        String.format(
            "http://%s:%d", localAiContainer.getHost(), localAiContainer.getMappedPort(8080));
    System.setProperty("segment-narration.ai.localai.base-url", baseUrl);
    // Set the model name to match what we installed
    System.setProperty("segment-narration.ai.localai.model", modelName);

    log.info("LocalAI Container started at: {}", baseUrl);
    log.info("Model installed: {}", modelName);
  }

  @PreDestroy
  public void stopLocalAiContainer() {
    if (localAiContainer != null) {
      localAiContainer.stop();
    }
  }
}

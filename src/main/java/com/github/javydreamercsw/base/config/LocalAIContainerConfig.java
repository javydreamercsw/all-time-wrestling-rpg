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
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.time.Duration;
import java.util.Collections;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

/**
 * Manages a LocalAI container for development using Testcontainers. This configuration is only
 * active when the non-test profile is enabled.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class LocalAIContainerConfig {

  private final AiSettingsService aiSettingsService;
  private final LocalAIStatusService statusService;
  @Getter private GenericContainer<?> localAiContainer;
  private boolean started = false;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    startLocalAiContainer();
  }

  public void startLocalAiContainer() {
    if (started) {
      return;
    }
    // Temporarily grant system-level privileges to fetch the model name
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
    try {
      UsernamePasswordAuthenticationToken systemAuth =
          new UsernamePasswordAuthenticationToken(
              "system", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(systemAuth);

      if (aiSettingsService.isLocalAIEnabled()) {
        String modelName = aiSettingsService.getLocalAIModel();
        String imageModelName = aiSettingsService.getLocalAIImageModel();
        if ((modelName != null && !modelName.isEmpty())
            || (imageModelName != null && !imageModelName.isEmpty())) {
          new Thread(() -> initializeAndStartContainer(modelName, imageModelName)).start();
        }
        started = true;
      }
    } finally {
      SecurityContextHolder.getContext().setAuthentication(originalAuth);
    }
  }

  private void initializeAndStartContainer(String modelName, String imageModelName) {
    try {
      statusService.setStatus(LocalAIStatusService.Status.STARTING);
      statusService.setMessage("LocalAI container is starting...");

      File modelsDir = new File("models");
      if (!modelsDir.exists()) {
        modelsDir.mkdirs();
      }

      String[] command;
      if (modelName != null
          && !modelName.isEmpty()
          && imageModelName != null
          && !imageModelName.isEmpty()) {
        command = new String[] {"run", modelName, imageModelName};
      } else if (modelName != null && !modelName.isEmpty()) {
        command = new String[] {"run", modelName};
      } else {
        command = new String[] {"run", imageModelName};
      }

      // Using the official LocalAI image
      localAiContainer =
          new GenericContainer<>("localai/localai:latest")
              .withExposedPorts(8080)
              .withFileSystemBind(modelsDir.getAbsolutePath(), "/build/models", BindMode.READ_WRITE)
              .withEnv("MODELS_PATH", "/build/models")
              .withCommand(command)
              .waitingFor(
                  new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/readyz").forStatusCode(200))
                      .withStartupTimeout(Duration.ofMinutes(30)));

      log.info(
          "Starting LocalAI container. This may take a while for the initial model download...");
      statusService.setStatus(LocalAIStatusService.Status.DOWNLOADING_MODEL);
      statusService.setMessage(
          "Downloading/installing AI model(s). This can take several minutes...");

      localAiContainer.start();

      String baseUrl =
          String.format(
              "http://%s:%d", localAiContainer.getHost(), localAiContainer.getMappedPort(8080));
      System.setProperty("ai.localai.base-url", baseUrl);
      if (modelName != null) {
        System.setProperty("ai.localai.model", modelName);
      }
      if (imageModelName != null) {
        System.setProperty("ai.localai.image-model", imageModelName);
      }

      statusService.setStatus(LocalAIStatusService.Status.READY);
      statusService.setMessage("LocalAI is ready at " + baseUrl);
      log.info("LocalAI Container started at: {}", baseUrl);
      if (modelName != null) log.info("Model '{}' is ready.", modelName);
      if (imageModelName != null) log.info("Image Model '{}' is ready.", imageModelName);

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

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
import com.github.javydreamercsw.management.service.GameSettingService;
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
  private final GameSettingService gameSettingService;

  @Getter
  @lombok.Setter(lombok.AccessLevel.PACKAGE)
  private GenericContainer<?> localAiContainer;

  private boolean started = false;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    startLocalAiContainer();
  }

  public synchronized void startLocalAiContainer() {
    startLocalAiContainer(false);
  }

  public synchronized void startLocalAiContainer(boolean force) {
    if (started && !force) {
      return;
    }
    started = true;
    new Thread(() -> runLocalAiContainer(force)).start();
  }

  @PreDestroy
  public synchronized void stopLocalAiContainer() {
    if (localAiContainer != null) {
      log.info("Stopping LocalAI container...");
      localAiContainer.stop();
      localAiContainer = null;
    }
    started = false;
    statusService.setStatus(LocalAIStatusService.Status.NOT_STARTED);
    statusService.setMessage("LocalAI is not initialized.");
  }

  public synchronized void forceRestartLocalAiContainer() {
    stopLocalAiContainer();
    startLocalAiContainer(true);
  }

  private void runLocalAiContainer(boolean force) {
    // Temporarily grant system-level privileges to fetch the model name
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
    try {
      UsernamePasswordAuthenticationToken systemAuth =
          new UsernamePasswordAuthenticationToken(
              "system", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(systemAuth);

      if (force || aiSettingsService.isLocalAIEnabled()) {
        String modelName = aiSettingsService.getLocalAIModel();
        String imageModelName = aiSettingsService.getLocalAIImageModel();
        if ((modelName != null && !modelName.isEmpty())
            || (imageModelName != null && !imageModelName.isEmpty())) {
          initializeAndStartContainer(modelName, imageModelName);
        }
      }
    } finally {
      SecurityContextHolder.getContext().setAuthentication(originalAuth);
    }
  }

  private void initializeAndStartContainer(String modelName, String imageModelName) {
    try {
      statusService.setStatus(LocalAIStatusService.Status.STARTING);
      statusService.setMessage("LocalAI container is starting...");

      File modelsDir = new File("data", "models");
      if (!modelsDir.exists()) {
        modelsDir.mkdirs();
      }

      File backendsDir = new File("data", "backends");
      if (!backendsDir.exists()) {
        backendsDir.mkdirs();
      }

      // Using the official LocalAI image with environment variables for model management
      String modelUrl = aiSettingsService.getLocalAIModelUrl();
      GenericContainer<?> container =
          new GenericContainer<>("localai/localai:latest-aio-cpu")
              .withExposedPorts(8080)
              .withFileSystemBind(modelsDir.getAbsolutePath(), "/build/models", BindMode.READ_WRITE)
              .withFileSystemBind(
                  backendsDir.getAbsolutePath(), "/build/backends", BindMode.READ_WRITE)
              .withEnv("MODELS_PATH", "/build/models")
              .withEnv("BACKENDS_PATH", "/build/backends");

      // Set model name for the API to recognize
      if (modelName != null && !modelName.isEmpty()) {
        container.withEnv("MODEL_NAME", modelName);
      }

      // Trigger download if model is specified by name or URL
      if (modelUrl != null && !modelUrl.isEmpty()) {
        container.withEnv("MODELS", modelUrl);
      } else if (modelName != null && !modelName.isEmpty()) {
        container.withEnv("MODELS", modelName);
      }

      if (imageModelName != null && !imageModelName.isEmpty()) {
        container.withEnv("IMAGE_MODEL", imageModelName);
      }

      localAiContainer =
          container.waitingFor(
              new WaitAllStrategy()
                  .withStrategy(Wait.forHttp("/readyz").forStatusCode(200))
                  .withStartupTimeout(Duration.ofMinutes(30)));

      log.info(
          "Starting LocalAI container. This may take a while for the initial model download...");
      statusService.setStatus(LocalAIStatusService.Status.DOWNLOADING_MODEL);
      statusService.setMessage(
          "Downloading/installing AI model(s). This can take several minutes...");

      localAiContainer.start();
      updateConfigurationFromContainer(modelName, imageModelName);

      statusService.setStatus(LocalAIStatusService.Status.READY);
      statusService.setMessage("LocalAI is ready at " + System.getProperty("ai.localai.base-url"));
      log.info("LocalAI Container started at: {}", System.getProperty("ai.localai.base-url"));
      if (modelName != null) log.info("Model '{}' is ready.", modelName);
      if (imageModelName != null) log.info("Image Model '{}' is ready.", imageModelName);

    } catch (Exception e) {
      log.error("Failed to start LocalAI container", e);
      statusService.setStatus(LocalAIStatusService.Status.FAILED);
      statusService.setMessage("LocalAI failed to start: " + e.getMessage());
    }
  }

  protected void updateConfigurationFromContainer(String modelName, String imageModelName) {
    String baseUrl =
        String.format(
            "http://%s:%d", localAiContainer.getHost(), localAiContainer.getMappedPort(8080));

    // Update system properties for immediate availability in this thread
    System.setProperty("ai.localai.base-url", baseUrl);
    if (modelName != null) {
      System.setProperty("ai.localai.model", modelName);
    }
    if (imageModelName != null) {
      System.setProperty("ai.localai.image-model", imageModelName);
    }

    // Update database settings so other services (like health check) see the correct URL
    gameSettingService.save("AI_LOCALAI_BASE_URL", baseUrl);
  }
}

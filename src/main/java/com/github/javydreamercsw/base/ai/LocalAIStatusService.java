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

import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocalAIStatusService {

  private final LocalAIConfigProperties config;
  private final HttpClient httpClient;

  @Autowired
  public LocalAIStatusService(LocalAIConfigProperties config) {
    this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  public LocalAIStatusService(LocalAIConfigProperties config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  @Getter @Setter private Status status = Status.NOT_STARTED;
  @Getter @Setter private String message = "LocalAI is not initialized.";
  private int failureCount = 0;
  private static final int MAX_FAILURES = 3;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper();

  public List<String> fetchAvailableModels() {
    List<String> models = new ArrayList<>();
    if (status != Status.READY) {
      return models;
    }

    try {
      String baseUrl = config.getBaseUrl();
      URI uri = URI.create(baseUrl + "/v1/models");
      HttpRequest request =
          HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(10)).GET().build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
        if (data != null) {
          for (Map<String, Object> model : data) {
            String id = (String) model.get("id");
            if (id != null) {
              models.add(id);
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to fetch models from LocalAI: {}", e.getMessage());
    }
    return models;
  }

  /**
   * Triggers model installation in the LocalAI container via the API.
   *
   * @param modelId The ID to assign to the model.
   * @param url The download URL (supports huggingface://, http://, etc.)
   * @return true if the request was accepted.
   */
  public boolean installModel(String modelId, String url) {
    if (status != Status.READY) {
      return false;
    }

    try {
      String baseUrl = config.getBaseUrl();
      URI uri = URI.create(baseUrl + "/models/apply");

      Map<String, String> body = Map.of("id", modelId, "url", url);
      String requestBody = objectMapper.writeValueAsString(body);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(uri)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      log.info("Install model request for {} returned status: {}", modelId, response.statusCode());
      return response.statusCode() == 200;
    } catch (Exception e) {
      log.error("Failed to trigger model installation: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Fetches the current background jobs (like model installations) from LocalAI.
   *
   * @return A list of maps containing job details (id, status, progress, etc.)
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> fetchInstallationJobs() {
    if (status != Status.READY) {
      return List.of();
    }

    try {
      String baseUrl = config.getBaseUrl();
      URI uri = URI.create(baseUrl + "/v1/models/jobs");
      HttpRequest request =
          HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(5)).GET().build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return objectMapper.readValue(response.body(), List.class);
      }
    } catch (Exception e) {
      log.warn("Failed to fetch installation jobs from LocalAI: {}", e.getMessage());
    }
    return List.of();
  }

  public enum Status {
    NOT_STARTED,
    STARTING,
    DOWNLOADING_MODEL,
    READY,
    FAILED
  }

  public boolean isReady() {
    return status == Status.READY;
  }

  /**
   * Performs an immediate health check of the LocalAI service.
   *
   * @return The updated status.
   */
  public Status checkHealth() {
    if (!config.isEnabled()) {
      status = Status.NOT_STARTED;
      message = "LocalAI is disabled in configuration.";
      return status;
    }

    String baseUrl = config.getBaseUrl();
    if (baseUrl == null || baseUrl.isEmpty()) {
      status = Status.FAILED;
      message = "LocalAI Base URL is not configured.";
      return status;
    }

    URI uri = URI.create(baseUrl + "/readyz");
    log.debug("Checking LocalAI health at: {}", uri);

    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(3)).GET().build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        status = Status.READY;
        message = "LocalAI is ready and healthy at " + baseUrl;
        failureCount = 0; // Reset on success
      } else {
        failureCount++;
        if (failureCount >= MAX_FAILURES) {
          status = Status.STARTING;
          message =
              "LocalAI is starting up at " + baseUrl + " (HTTP " + response.statusCode() + ")";
        }
      }
    } catch (Exception e) {
      failureCount++;
      String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      if (failureCount >= MAX_FAILURES || status != Status.READY) {
        status = Status.FAILED;
        message = "Cannot connect to LocalAI at " + baseUrl + "/readyz: " + errorMsg;
      }
      log.warn("LocalAI health check failed at {}/readyz: {}", baseUrl, errorMsg);
    }
    return status;
  }

  @Scheduled(fixedDelay = 10_000) // Check every 10 seconds
  public void checkStatus() {
    if (!config.isEnabled()) {
      status = Status.NOT_STARTED;
      message = "LocalAI is disabled in configuration.";
      return;
    }

    // If already ready, only check once every 2 minutes to reduce noise
    if (status == Status.READY && (System.currentTimeMillis() % 120_000 > 10_000)) {
      return;
    }

    checkHealth();
  }
}

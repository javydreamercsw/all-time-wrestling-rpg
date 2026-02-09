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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocalAIStatusService {

  private final LocalAIConfigProperties config;
  private final HttpClient httpClient;

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
        message = "LocalAI is ready.";
        failureCount = 0; // Reset on success
      } else {
        failureCount++;
        if (failureCount >= MAX_FAILURES) {
          status = Status.STARTING;
          message = "LocalAI is starting up (HTTP " + response.statusCode() + ")";
        }
      }
    } catch (Exception e) {
      failureCount++;
      if (failureCount >= MAX_FAILURES || status != Status.READY) {
        status = Status.FAILED;
        String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        message = "Cannot connect to LocalAI at " + baseUrl + "/readyz: " + errorMsg;
      }
      log.debug(
          "LocalAI health check failed at " + baseUrl + " (failure count: " + failureCount + ")",
          e);
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

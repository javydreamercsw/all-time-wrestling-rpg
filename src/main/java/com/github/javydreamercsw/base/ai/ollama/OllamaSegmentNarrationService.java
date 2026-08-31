/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SegmentNarrationService backed by a local Ollama instance via its OpenAI-compatible
 * chat-completions endpoint ({@code /v1/chat/completions}).
 *
 * <p>Configuration (environment variables take priority; UI settings are the fallback):
 *
 * <ul>
 *   <li>{@code OLLAMA_BASE_URL} — e.g. {@code http://localhost:11434}. When unset and no {@code
 *       AI_OLLAMA_BASE_URL} game setting is configured, the provider reports unavailable.
 *   <li>{@code OLLAMA_MODEL} — model tag, default {@code llama3.2:1b}.
 *   <li>{@code OLLAMA_TIMEOUT_SECONDS} — per-request timeout, default 300.
 * </ul>
 */
@Service
@Slf4j
public class OllamaSegmentNarrationService extends AbstractSegmentNarrationService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AiSettingsService aiSettingsService;

  public OllamaSegmentNarrationService(final AiSettingsService aiSettingsService) {
    this.aiSettingsService = aiSettingsService;
  }

  String baseUrl() {
    String env = System.getenv("OLLAMA_BASE_URL");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    String setting = aiSettingsService.getOllamaBaseUrl();
    return (setting != null && !setting.isBlank()) ? setting.trim() : null;
  }

  String model() {
    String env = System.getenv("OLLAMA_MODEL");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    String setting = aiSettingsService.getOllamaModel();
    return (setting != null && !setting.isBlank()) ? setting.trim() : "llama3.2:1b";
  }

  private int timeoutSeconds() {
    String timeout = System.getenv("OLLAMA_TIMEOUT_SECONDS");
    return timeout == null || timeout.isBlank()
        ? aiSettingsService.getAiTimeout()
        : Integer.parseInt(timeout.trim());
  }

  @Override
  public String getProviderName() {
    return "Ollama";
  }

  @Override
  public boolean isAvailable() {
    String url = baseUrl();
    return url != null && !url.isBlank();
  }

  @Override
  protected String callAIProvider(@NonNull final String prompt) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503,
          "Service Unavailable",
          getProviderName(),
          "OLLAMA_BASE_URL is not set and no AI_OLLAMA_BASE_URL game setting is configured");
    }

    try {
      Map<String, Object> requestBody =
          Map.of(
              "model",
              model(),
              "messages",
              List.of(
                  Map.of("role", "system", "content", getSystemMessage(prompt)),
                  Map.of("role", "user", "content", prompt)),
              "temperature",
              0.2,
              "max_tokens",
              4096);

      String jsonBody = objectMapper.writeValueAsString(requestBody);
      int timeout = timeoutSeconds();

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl().replaceAll("/$", "") + "/v1/chat/completions"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer ollama")
              .timeout(Duration.ofSeconds(timeout))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      HttpResponse<String> response =
          getHttpClient(timeout).send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new AIServiceException(
            response.statusCode(),
            "Ollama API Error",
            getProviderName(),
            "Ollama returned an error: " + response.body());
      }

      JsonNode root = objectMapper.readTree(response.body());
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (content.isMissingNode() || content.isNull()) {
        throw new AIServiceException(
            502, "Bad Gateway", getProviderName(), "No content in Ollama response");
      }
      return content.asText().trim();
    } catch (AIServiceException e) {
      throw e;
    } catch (HttpTimeoutException e) {
      throw new AIServiceException(504, "Gateway Timeout", getProviderName(), e.getMessage(), e);
    } catch (Exception e) {
      log.error("Failed to call Ollama for segment narration", e);
      if (Thread.currentThread().isInterrupted()) {
        Thread.currentThread().interrupt();
      }
      throw new AIServiceException(
          500, "Internal Server Error", getProviderName(), e.getMessage(), e);
    }
  }
}

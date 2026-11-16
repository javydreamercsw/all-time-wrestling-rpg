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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.LinkedHashMap;
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
 *   <li>{@code OLLAMA_MAX_TOKENS} — max output tokens, default 4096. Reasoning ("thinking") tokens
 *       count toward this limit.
 *   <li>{@code OLLAMA_DISABLE_THINKING} — when {@code true}, appends {@code /no_think} to the
 *       system message (Qwen3-family hybrid models).
 * </ul>
 *
 * <p>Extra request parameters (merged into the request body, overriding the defaults above) come
 * from the {@code AI_OLLAMA_EXTRA_PARAMS} game setting only — no environment variable, since JSON
 * through an env var is a quoting footgun. The reserved keys {@code model} and {@code messages} are
 * stripped from it.
 */
@Service
@Slf4j
public class OllamaSegmentNarrationService extends AbstractSegmentNarrationService {

  private static final String RESERVED_MODEL_KEY = "model";
  private static final String RESERVED_MESSAGES_KEY = "messages";

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
    return setting != null && !setting.isBlank() ? setting.trim() : null;
  }

  String model() {
    String env = System.getenv("OLLAMA_MODEL");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    String setting = aiSettingsService.getOllamaModel();
    return setting != null && !setting.isBlank() ? setting.trim() : "llama3.2:1b";
  }

  private int timeoutSeconds() {
    String timeout = System.getenv("OLLAMA_TIMEOUT_SECONDS");
    return timeout == null || timeout.isBlank()
        ? aiSettingsService.getAiTimeout()
        : Integer.parseInt(timeout.trim());
  }

  private int maxTokens() {
    String env = System.getenv("OLLAMA_MAX_TOKENS");
    if (env != null && !env.isBlank()) {
      return Integer.parseInt(env.trim());
    }
    return aiSettingsService.getOllamaMaxTokens();
  }

  private boolean disableThinking() {
    String env = System.getenv("OLLAMA_DISABLE_THINKING");
    if (env != null && !env.isBlank()) {
      return Boolean.parseBoolean(env.trim());
    }
    return aiSettingsService.isOllamaDisableThinking();
  }

  Map<String, Object> extraParams() {
    String setting = aiSettingsService.getOllamaExtraParams();
    if (setting == null || setting.isBlank()) {
      return Map.of();
    }
    try {
      JsonNode tree = objectMapper.readTree(setting);
      if (!tree.isObject()) {
        log.warn("AI_OLLAMA_EXTRA_PARAMS must be a JSON object; ignoring value");
        return Map.of();
      }
      return objectMapper.convertValue(tree, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse AI_OLLAMA_EXTRA_PARAMS as JSON; ignoring value", e);
      return Map.of();
    }
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

  /**
   * Appends the {@code /no_think} soft switch when thinking is disabled. Qwen3-family hybrid models
   * stop emitting reasoning tokens; other models treat it as harmless trailing text.
   */
  @Override
  protected String getSystemMessage(@NonNull final String prompt) {
    String base = super.getSystemMessage(prompt);
    return disableThinking() ? base + "\n\n/no_think" : base;
  }

  /** Builds the request body: named defaults first, then extra params merged as the last word. */
  Map<String, Object> buildRequestBody(final String prompt) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model());
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", getSystemMessage(prompt)),
            Map.of("role", "user", "content", prompt)));
    body.put("temperature", 0.2);
    body.put("max_tokens", maxTokens());

    Map<String, Object> extras = new LinkedHashMap<>(extraParams());
    List<String> reserved =
        extras.keySet().stream()
            .filter(key -> RESERVED_MODEL_KEY.equals(key) || RESERVED_MESSAGES_KEY.equals(key))
            .toList();
    if (!reserved.isEmpty()) {
      log.warn(
          "Ignoring reserved key(s) {} in AI_OLLAMA_EXTRA_PARAMS (they would corrupt the request)",
          reserved);
      reserved.forEach(extras::remove);
    }
    body.putAll(extras);
    return body;
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
      Map<String, Object> requestBody = buildRequestBody(prompt);
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
      if ("length".equals(root.path("choices").path(0).path("finish_reason").asText())) {
        log.warn(
            "Ollama narration truncated at max_tokens={} (finish_reason=length); "
                + "increase AI_OLLAMA_MAX_TOKENS or enable Disable Thinking",
            maxTokens());
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

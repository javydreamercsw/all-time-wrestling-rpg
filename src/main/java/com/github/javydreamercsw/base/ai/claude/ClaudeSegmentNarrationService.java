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
package com.github.javydreamercsw.base.ai.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude implementation of the SegmentNarrationService interface. Uses Anthropic's Claude
 * API for wrestling segment narration with rich context.
 */
@Service
@Profile("!test & !e2e")
@Slf4j
public class ClaudeSegmentNarrationService extends AbstractSegmentNarrationService {

  @Autowired private ObjectMapper objectMapper;
  private final Environment environment;
  private final AiSettingsService aiSettingsService;

  @Autowired
  public ClaudeSegmentNarrationService(
      final Environment environment, final AiSettingsService aiSettingsService) {
    this.aiSettingsService = aiSettingsService;
    this.environment = environment;
  }

  @Override
  protected String callAIProvider(@NonNull final String prompt) {
    return callClaude(prompt);
  }

  @Override
  public String getProviderName() {
    return "Anthropic Claude";
  }

  @Override
  public boolean isAvailable() {
    if (!aiSettingsService.isClaudeEnabled()) {
      return false;
    }
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      return false;
    }
    String apiKey = aiSettingsService.getClaudeApiKey();
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the Claude API with the given prompt. */
  private String callClaude(@NonNull final String prompt) {
    try {
      String fullApiUrl = aiSettingsService.getClaudeApiUrl();
      String modelName = aiSettingsService.getClaudeModelName();
      int timeout = aiSettingsService.getAiTimeout();

      Map<String, Object> requestBody =
          Map.of(
              "model",
              modelName,
              "max_tokens",
              4000,
              "messages",
              List.of(
                  Map.of("role", "user", "content", getSystemMessage(prompt) + "\n\n" + prompt)));

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(fullApiUrl))
              .header("Content-Type", "application/json")
              .header("x-api-key", aiSettingsService.getClaudeApiKey())
              .header("anthropic-version", "2023-06-01")
              .timeout(Duration.ofSeconds(timeout))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      HttpResponse<String> response =
          getHttpClient(timeout).send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        throw new AIServiceException(
            response.statusCode(),
            "Claude API Error",
            getProviderName(),
            "Claude API returned an error: " + response.body());
      }

    } catch (Exception e) {
      log.error("Failed to call Claude API for segment narration", e);
      if (e instanceof java.net.http.HttpTimeoutException) {
        throw new AIServiceException(504, "Gateway Timeout", getProviderName(), e.getMessage(), e);
      }
      throw new AIServiceException(
          500, "Internal Server Error", getProviderName(), e.getMessage(), e);
    }
  }

  /** Extracts the content from Claude API response. */
  private String extractContentFromResponse(@NonNull final String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      // Record token usage if available
      if (getPerformanceMonitoringService() != null && response.containsKey("usage")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
          int input = (int) usage.getOrDefault("input_tokens", 0);
          int output = (int) usage.getOrDefault("output_tokens", 0);
          getPerformanceMonitoringService().recordTokenUsage(getProviderName(), input, output);
        }
      }

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

      if (content != null && !content.isEmpty()) {
        return (String) content.get(0).get("text");
      }

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse Claude response", e);
      return "Error parsing AI response: " + e.getMessage();
    }
  }
}

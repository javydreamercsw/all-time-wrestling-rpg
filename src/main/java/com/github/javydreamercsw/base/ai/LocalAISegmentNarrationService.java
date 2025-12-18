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

import com.github.javydreamercsw.base.ai.prompt.PromptGenerator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Segment narration service using a local AI provider. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalAISegmentNarrationService implements SegmentNarrationService {

  private final SegmentNarrationConfig config;
  private final Environment environment;

  @Override
  public String getProviderName() {
    return "LocalAI";
  }

  @Override
  public boolean isAvailable() {
    return getBaseUrl() != null && config.getAi().getLocalai() != null;
  }

  private String getBaseUrl() {
    // Prefer the system property/environment variable if set (e.g. by Testcontainers)
    String dynamicUrl = environment.getProperty("segment-narration.ai.localai.base-url");
    if (dynamicUrl != null && !dynamicUrl.isEmpty()) {
      return dynamicUrl;
    }
    // Fallback to the configuration property
    if (config.getAi().getLocalai() != null) {
      return config.getAi().getLocalai().getBaseUrl();
    }
    return null;
  }

  @Override
  public String narrateSegment(@NonNull SegmentNarrationContext segmentContext) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503,
          "Service Unavailable",
          getProviderName(),
          "LocalAI service is not configured.",
          null);
    }
    String prompt = new PromptGenerator().generateMatchNarrationPrompt(segmentContext);
    return generateText(prompt);
  }

  @Override
  public String summarizeNarration(@NonNull String narration) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503,
          "Service Unavailable",
          getProviderName(),
          "LocalAI service is not configured.",
          null);
    }
    String prompt = new PromptGenerator().generateSummaryPrompt(narration);
    return generateText(prompt);
  }

  @Override
  public String generateText(@NonNull String prompt) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503,
          "Service Unavailable",
          getProviderName(),
          "LocalAI service is not configured.",
          null);
    }
    try {
      log.debug("Generating text with LocalAI. Prompt length: {}", prompt.length());
      log.trace("Full Prompt: {}", prompt);

      HttpClient client = HttpClient.newHttpClient();
      String requestBody =
          new JSONObject()
              .put("model", config.getAi().getLocalai().getModel())
              .put("prompt", prompt)
              .put("temperature", 0.7)
              .toString();

      String baseUrl = getBaseUrl();
      log.debug("Using LocalAI Base URL: {}", baseUrl);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/v1/completions"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      log.debug("LocalAI response status code: {}", response.statusCode());
      log.debug("LocalAI response body: {}", response.body());

      if (response.statusCode() == 200) {
        JSONObject jsonResponse = new JSONObject(response.body());
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (!choices.isEmpty()) {
          String text = choices.getJSONObject(0).getString("text").trim();
          if (text.isEmpty()) {
            log.warn("LocalAI returned an empty text string.");
          }
          return text;
        }
      }
      throw new AIServiceException(
          response.statusCode(),
          "API Error",
          getProviderName(),
          "Failed to get a valid response from LocalAI. Response: " + response.body(),
          null);
    } catch (Exception e) {
      log.error("Error communicating with LocalAI service", e);
      throw new AIServiceException(
          500,
          "Internal Server Error",
          getProviderName(),
          "Failed to communicate with LocalAI service.",
          e);
    }
  }
}

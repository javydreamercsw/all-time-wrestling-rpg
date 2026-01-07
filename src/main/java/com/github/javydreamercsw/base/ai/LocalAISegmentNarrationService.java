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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.ai.prompt.PromptGenerator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Segment narration service using a local AI provider. */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("(!test & !e2e) or local-ai-it")
public class LocalAISegmentNarrationService implements SegmentNarrationService {

  private final LocalAIConfigProperties config;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String getProviderName() {
    return "LocalAI";
  }

  @Override
  public boolean isAvailable() {
    return getBaseUrl() != null && !getBaseUrl().isEmpty();
  }

  private String getBaseUrl() {
    return config.getBaseUrl();
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
    String prompt = new PromptGenerator().generateSimplifiedMatchNarrationPrompt(segmentContext);
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
      log.debug("Full Prompt: {}", prompt);

      HttpClient client = HttpClient.newHttpClient();

      Map<String, Object> systemMessage = Map.of("role", "system", "content", getSystemMessage());
      Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

      Map<String, Object> requestBodyMap =
          Map.of(
              "model",
              config.getModel(),
              "messages",
              List.of(systemMessage, userMessage),
              "temperature",
              0.7);

      String requestBody = objectMapper.writeValueAsString(requestBodyMap);

      String baseUrl = getBaseUrl();
      log.debug("Using LocalAI Base URL: {}", baseUrl);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/v1/chat/completions"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      log.debug("LocalAI response status code: {}", response.statusCode());
      log.debug("LocalAI response body: {}", response.body());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
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

  private String getSystemMessage() {
    return "You are a creative and knowledgeable wrestling commentator. Your task is to generate a "
        + "detailed, play-by-play narration for a wrestling segment based on the context provided. "
        + "Emphasize the wrestlers' personalities, the history between them, the significance of "
        + "the segment, and the drama of the action. Use vivid language and capture the excitement "
        + "of a live wrestling broadcast.";
  }

  @SuppressWarnings("unchecked")
  private String extractContentFromResponse(@NonNull String responseBody) throws Exception {
    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
    if (choices != null && !choices.isEmpty()) {
      Map<String, Object> firstChoice = choices.get(0);
      Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
      if (message != null && message.containsKey("content")) {
        String content = (String) message.get("content");
        if (content != null && !content.trim().isEmpty()) {
          return content.trim();
        }
      }
    }
    log.warn("LocalAI returned a response with no usable content.");
    return "";
  }
}

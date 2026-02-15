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
package com.github.javydreamercsw.base.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Google Gemini implementation of the SegmentNarrationService interface. Uses Google's Gemini API
 * for wrestling segment narration with rich context.
 */
@Service
@Profile("!test & !e2e")
@Slf4j
public class GeminiSegmentNarrationService extends AbstractSegmentNarrationService {

  private final ObjectMapper objectMapper;
  private final GeminiConfigProperties geminiConfigProperties;
  private final Environment environment;
  private final AiSettingsService aiSettingsService;

  @Autowired // Autowire the configuration properties
  public GeminiSegmentNarrationService(
      GeminiConfigProperties geminiConfigProperties,
      Environment environment,
      AiSettingsService aiSettingsService) {
    this.aiSettingsService = aiSettingsService;
    this.objectMapper = new ObjectMapper();
    this.geminiConfigProperties = geminiConfigProperties;
    this.environment = environment;
  }

  @Override
  protected String callAIProvider(@NonNull String prompt) {
    return callGemini(prompt);
  }

  @Override
  public String getProviderName() {
    return "Google Gemini";
  }

  @Override
  public boolean isAvailable() {
    if (!aiSettingsService.isGeminiEnabled()) {
      return false;
    }
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      return false;
    }
    String apiKey = geminiConfigProperties.getApiKey();
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  @Override
  public String generateText(@NonNull String prompt) {
    return callGemini(prompt);
  }

  /** Makes a call to the Gemini API with the given prompt. */
  private String callGemini(@NonNull String prompt) {
    try {
      // Use configured API URL and model name
      String modelName = geminiConfigProperties.getModelName();
      String apiUrl = geminiConfigProperties.getApiUrl();
      String fullApiUrl = apiUrl + modelName + ":generateContent";

      String apiKey = geminiConfigProperties.getApiKey();
      if (apiKey != null) {
        apiKey = apiKey.trim();
      }

      // Logging for troubleshooting
      int keyLen = (apiKey != null) ? apiKey.length() : 0;
      String keyStart = (keyLen > 4) ? apiKey.substring(0, 4) : "***";
      String keyEnd = (keyLen > 4) ? apiKey.substring(keyLen - 4) : "***";

      log.debug(
          "Gemini Request - URL: {}, Model: {}, Key Length: {}, Key: {}...{}",
          fullApiUrl,
          modelName,
          keyLen,
          keyStart,
          keyEnd);

      String url = fullApiUrl + "?key=" + apiKey;

      // Create request body for Gemini API
      Map<String, Object> requestBody =
          Map.of(
              "contents",
              List.of(
                  Map.of(
                      "parts",
                      List.of(Map.of("text", getSystemMessage(prompt) + "\n\n" + prompt)))),
              "generationConfig",
              Map.of(
                  "temperature",
                  0.8, // Higher creativity for storytelling
                  "topK",
                  40,
                  "topP",
                  0.95,
                  "maxOutputTokens",
                  8000, // Much longer output for detailed segment narration (up to ~6000 words)
                  "stopSequences",
                  List.of()),
              "safetySettings",
              List.of(
                  Map.of(
                      "category",
                      "HARM_CATEGORY_HARASSMENT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_HATE_SPEECH",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_DANGEROUS_CONTENT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE")));

      String jsonBody = objectMapper.writeValueAsString(requestBody);
      log.debug("Gemini Request Body: {}", jsonBody);

      // Create HTTP request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofSeconds(geminiConfigProperties.getTimeout()))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();
      // Send request and get response
      HttpResponse<InputStream> response =
          getHttpClient(geminiConfigProperties.getTimeout())
              .send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() == 200) {
        try (InputStream responseBody = response.body()) {
          String text =
              new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8))
                  .lines()
                  .collect(Collectors.joining("\n"));
          return extractContentFromResponse(text);
        }
      } else {
        // Throw custom exception for AI service errors
        try (InputStream responseBody = response.body()) {
          String errorText =
              new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8))
                  .lines()
                  .collect(Collectors.joining("\n"));
          throw new AIServiceException(
              response.statusCode(), "Gemini API Error", getProviderName(), errorText);
        }
      }

    } catch (Exception e) {
      log.error("Failed to call Gemini API for segment narration", e);
      if (e instanceof java.net.http.HttpTimeoutException) {
        throw new AIServiceException(504, "Gateway Timeout", getProviderName(), e.getMessage(), e);
      }
      // Re-throw as custom exception
      throw new AIServiceException(
          500, "Internal Server Error", getProviderName(), e.getMessage(), e);
    }
  }

  /** Extracts the content from Gemini API response. */
  private String extractContentFromResponse(@NonNull String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

      if (candidates != null && !candidates.isEmpty()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        if (parts != null && !parts.isEmpty()) {
          return (String) parts.get(0).get("text");
        }
      }

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse Gemini response", e);
      return "Error parsing AI response: " + e.getMessage();
    }
  }
}

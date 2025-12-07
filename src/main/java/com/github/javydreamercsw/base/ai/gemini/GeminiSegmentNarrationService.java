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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Google Gemini implementation of the SegmentNarrationService interface. Uses Google's Gemini API
 * for wrestling segment narration with rich context.
 */
@Service
@Slf4j
public class GeminiSegmentNarrationService extends AbstractSegmentNarrationService {

  private static final Duration TIMEOUT =
      Duration.ofSeconds(60); // Longer timeout for segment narration

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final GeminiConfigProperties geminiConfigProperties;
  private final Environment environment;

  @Autowired // Autowire the configuration properties
  public GeminiSegmentNarrationService(
      GeminiConfigProperties geminiConfigProperties, Environment environment) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("GEMINI_API_KEY");
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
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      return false;
    }
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
      String fullApiUrl =
          geminiConfigProperties.getApiUrl()
              + geminiConfigProperties.getModelName()
              + ":generateContent";
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
                  4000, // Much longer output for detailed segment narration (up to ~3000 words)
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

      // Create HTTP request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .timeout(TIMEOUT)
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      // Send request and get response
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        // Throw custom exception for AI service errors
        throw new AIServiceException(
            response.statusCode(),
            "Gemini API Error",
            getProviderName(),
            "Gemini API returned an error: " + response.body());
      }

    } catch (Exception e) {
      log.error("Failed to call Gemini API for segment narration", e);
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

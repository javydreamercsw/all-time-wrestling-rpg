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
package com.github.javydreamercsw.base.ai.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Image generation service using LocalAI. */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("(!test & !e2e) or local-ai-it")
public class LocalAIImageGenerationService implements ImageGenerationService {

  private final LocalAIConfigProperties config;
  private final LocalAIStatusService statusService;
  private final AiSettingsService aiSettings;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String generateImage(@NonNull ImageRequest request) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503, "Service Unavailable", getProviderName(), "LocalAI image service is not available.");
    }

    try {
      log.info("Generating image with LocalAI. Prompt: {}", request.getPrompt());

      HttpClient client =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(aiSettings.getAiTimeout()))
              .build();

      String model =
          request.getModel() != null ? request.getModel() : aiSettings.getLocalAIImageModel();

      Map<String, Object> requestBodyMap =
          Map.of(
              "prompt", request.getPrompt(),
              "size", request.getSize(),
              "n", request.getN(),
              "model", model,
              "response_format", request.getResponseFormat());

      String requestBody = objectMapper.writeValueAsString(requestBodyMap);
      String baseUrl = config.getBaseUrl();

      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/v1/images/generations"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofSeconds(aiSettings.getAiTimeout()))
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractImageFromResponse(response.body(), request.getResponseFormat());
      }

      throw new AIServiceException(
          response.statusCode(),
          "API Error",
          getProviderName(),
          "Failed to generate image: " + response.body());

    } catch (Exception e) {
      log.error("Error generating image with LocalAI", e);
      throw new AIServiceException(
          500, "Internal Server Error", getProviderName(), "Error during image generation", e);
    }
  }

  @Override
  public String getProviderName() {
    return "LocalAI";
  }

  @Override
  public boolean isAvailable() {
    return config.isEnabled() && statusService.isReady();
  }

  private String extractImageFromResponse(String responseBody, String format) throws Exception {
    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
    List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
    if (data != null && !data.isEmpty()) {
      Map<String, Object> firstImage = data.get(0);
      if ("b64_json".equalsIgnoreCase(format)) {
        return (String) firstImage.get("b64_json");
      } else {
        return (String) firstImage.get("url");
      }
    }
    throw new RuntimeException("No image data found in response");
  }
}

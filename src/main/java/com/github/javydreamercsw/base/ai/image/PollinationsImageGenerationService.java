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

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Image generation service using Pollinations.ai (Free or Paid). */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test & !e2e")
public class PollinationsImageGenerationService implements ImageGenerationService {

  private final AiSettingsService aiSettings;

  @Override
  public String generateImage(@NonNull ImageRequest request) {
    log.info("Generating image with Pollinations. Prompt: {}", request.getPrompt());

    String encodedPrompt = URLEncoder.encode(request.getPrompt(), StandardCharsets.UTF_8);
    // Use gen.pollinations.ai which is the new unified API supporting keys
    String url = "https://gen.pollinations.ai/image/" + encodedPrompt;

    // Add default parameters
    url += "?nologo=true";

    // Add model if specified
    if (request.getModel() != null && !request.getModel().isEmpty()) {
      url += "&model=" + URLEncoder.encode(request.getModel(), StandardCharsets.UTF_8);
    } else {
      // Default to flux if not specified, it's generally better
      url += "&model=flux";
    }

    // Add size
    if (request.getSize() != null && request.getSize().contains("x")) {
      String[] parts = request.getSize().split("x");
      if (parts.length == 2) {
        url += "&width=" + parts[0] + "&height=" + parts[1];
      }
    }

    String apiKey = aiSettings.getPollinationsApiKey();
    if (apiKey != null && !apiKey.isEmpty()) {
      // If we have an API key, we must fetch the image server-side to pass the header safely
      // and then return it as a Data URI (Base64).
      log.debug("Using Pollinations API Key for generation.");
      try {
        HttpClient client =
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(aiSettings.getAiTimeout()))
                .build();

        HttpRequest httpRequest =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like"
                        + " Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header(
                    "Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(Duration.ofSeconds(aiSettings.getAiTimeout()))
                .GET()
                .build();

        HttpResponse<byte[]> response =
            client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
          String base64Image = Base64.getEncoder().encodeToString(response.body());
          String mimeType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
          return "data:" + mimeType + ";base64," + base64Image;
        } else {
          throw new AIServiceException(
              response.statusCode(),
              "Pollinations API Error",
              getProviderName(),
              "Failed to generate image: HTTP " + response.statusCode() + " - " + url);
        }

      } catch (Exception e) {
        log.error("Error generating image with Pollinations API Key", e);
        throw new AIServiceException(
            500, "Internal Server Error", getProviderName(), "Error during image generation", e);
      }
    }

    // Fallback to direct URL if no key (though it might be restricted)
    return url;
  }

  @Override
  public String getProviderName() {
    return "Pollinations";
  }

  @Override
  public boolean isAvailable() {
    return aiSettings.isPollinationsEnabled();
  }
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"local-ai-it"})
class LocalAIImageGenerationServiceIT {

  @Autowired private LocalAIImageGenerationService localAIImageGenerationService;
  @Autowired private LocalAIConfigProperties config;

  @Test
  void testServiceAvailability() {
    // Only run this test if LocalAI is enabled in config
    if (config.isEnabled()) {
      assertTrue(
          localAIImageGenerationService.isAvailable(), "LocalAI service should be available");
    } else {
      assertFalse(
          localAIImageGenerationService.isAvailable(), "LocalAI service should not be available");
    }
  }

  @Test
  void testGenerateImage_success() {
    if (config.isEnabled() && localAIImageGenerationService.isAvailable()) {
      String prompt = "A futuristic city in the style of cyberpunk";
      ImageGenerationService.ImageRequest request =
          ImageGenerationService.ImageRequest.builder().prompt(prompt).build();

      String imageUrl = localAIImageGenerationService.generateImage(request);
      assertNotNull(imageUrl);
      assertTrue(
          imageUrl.startsWith("http") || imageUrl.startsWith("data:image"),
          "Generated image should be a URL or base64 data URI");
    }
  }

  @Test
  void testGenerateImage_withCustomModel_success() {
    if (config.isEnabled() && localAIImageGenerationService.isAvailable()) {
      // Assuming 'stablediffusion' or similar is a valid image generation model for LocalAI
      String prompt = "A serene landscape with a small cabin";
      String customModel = "stablediffusion"; // Use a known working model for LocalAI if available
      ImageGenerationService.ImageRequest request =
          ImageGenerationService.ImageRequest.builder().prompt(prompt).model(customModel).build();

      String imageUrl = localAIImageGenerationService.generateImage(request);
      assertNotNull(imageUrl);
      assertTrue(
          imageUrl.startsWith("http") || imageUrl.startsWith("data:image"),
          "Generated image should be a URL or base64 data URI");
    }
  }

  // TODO: Add tests for error scenarios (e.g., invalid prompt, API error from LocalAI)
  // This would require mocking or setting up a specific LocalAI response for errors.
}

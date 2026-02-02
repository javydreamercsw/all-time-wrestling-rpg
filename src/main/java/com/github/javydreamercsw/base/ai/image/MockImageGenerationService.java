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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Mock image generation service for testing. */
@Service
@Profile({"test", "e2e"})
@Primary
@Slf4j
public class MockImageGenerationService implements ImageGenerationService {

  @Override
  public String generateImage(@NonNull ImageRequest request) {
    log.info("Mock AI generating image for prompt: {}", request.getPrompt());
    // Return a sample base64 placeholder or a dummy URL
    if ("b64_json".equalsIgnoreCase(request.getResponseFormat())) {
      return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="; // 1x1 white pixel
    }
    return "https://via.placeholder.com/1024x1024.png?text=Mock+AI+Image";
  }

  @Override
  public String getProviderName() {
    return "Mock AI";
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}

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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Interface for AI-powered image generation. */
public interface ImageGenerationService {

  /**
   * Generates an image based on the provided request.
   *
   * @param request The image generation parameters.
   * @return The URL of the generated image or base64 data.
   */
  String generateImage(@NonNull ImageRequest request);

  /**
   * Gets the provider name (e.g., "LocalAI", "OpenAI").
   *
   * @return The provider name.
   */
  String getProviderName();

  /**
   * Checks if the service is available.
   *
   * @return true if available.
   */
  boolean isAvailable();

  @Getter
  @Builder
  class ImageRequest {
    private String prompt;
    @Builder.Default private String size = "1024x1024";
    @Builder.Default private int n = 1;
    @Builder.Default private String responseFormat = "url"; // "url" or "b64_json"
    private String model;
  }
}

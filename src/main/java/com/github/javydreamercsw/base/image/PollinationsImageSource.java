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
package com.github.javydreamercsw.base.image;

import com.github.javydreamercsw.base.ai.image.ImageGenerationService;
import com.github.javydreamercsw.base.ai.image.ImageGenerationService.ImageRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Image source that dynamically generates images using Pollinations AI. */
@Component
@Slf4j
public class PollinationsImageSource implements ImageSource {

  private final ImageGenerationService imageGenerationService;

  @org.springframework.beans.factory.annotation.Autowired
  public PollinationsImageSource(
      @org.springframework.beans.factory.annotation.Qualifier("pollinationsImageGenerationService") ImageGenerationService imageGenerationService) {
    this.imageGenerationService = imageGenerationService;
  }

  @Override
  public Optional<String> resolveImage(String name, ImageCategory category) {
    if (!imageGenerationService.isAvailable()) {
      return Optional.empty();
    }

    log.info("Generating dynamic image for {} in category {}", name, category);
    String prompt =
        String.format(
            "A professional wrestling %s named '%s', high quality, sports entertainment style",
            category.name().toLowerCase(), name);

    try {
      String url =
          imageGenerationService.generateImage(ImageRequest.builder().prompt(prompt).build());
      return Optional.of(url);
    } catch (Exception e) {
      log.error("Failed to generate dynamic image for {}", name, e);
      return Optional.empty();
    }
  }

  @Override
  public int getPriority() {
    return 1000; // Lowest priority, last resort before generic fallback
  }
}

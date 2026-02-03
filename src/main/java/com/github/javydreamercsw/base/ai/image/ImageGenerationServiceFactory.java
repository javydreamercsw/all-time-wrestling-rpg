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

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Factory for selecting and using the best available image generation service. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationServiceFactory {

  private final List<ImageGenerationService> availableServices;

  /**
   * Generates an image using the best available service.
   *
   * @param request The image generation parameters.
   * @return The image data (URL or base64).
   */
  public String generateImage(@NonNull ImageGenerationService.ImageRequest request) {
    List<ImageGenerationService> services = getAvailableServicesInPriorityOrder();
    Exception lastException = null;

    for (ImageGenerationService service : services) {
      try {
        log.debug("Attempting to generate image with provider: {}", service.getProviderName());
        return service.generateImage(request);
      } catch (Exception e) {
        lastException = e;
        log.warn(
            "AI service provider {} failed to generate image: {}",
            service.getProviderName(),
            e.getMessage());
      }
    }
    throw new RuntimeException("All AI image providers failed to generate image.", lastException);
  }

  /**
   * Gets the best available image generation service.
   *
   * @return The best available service, or null if none.
   */
  public ImageGenerationService getBestAvailableService() {
    List<ImageGenerationService> sortedServices = getAvailableServicesInPriorityOrder();
    if (!sortedServices.isEmpty()) {
      return sortedServices.get(0);
    }
    return null;
  }

  /**
   * Gets available services in priority order.
   *
   * @return Sorted list of available services.
   */
  public List<ImageGenerationService> getAvailableServicesInPriorityOrder() {
    String[] priorityOrder = {"OpenAI", "Pollinations", "LocalAI", "Mock"};
    List<ImageGenerationService> sortedServices = new java.util.ArrayList<>();

    for (String provider : priorityOrder) {
      for (ImageGenerationService service : availableServices) {
        if (service.getProviderName().toLowerCase().contains(provider.toLowerCase())
            && service.isAvailable()) {
          sortedServices.add(service);
        }
      }
    }

    // Add any other available services
    for (ImageGenerationService service : availableServices) {
      if (service.isAvailable() && !sortedServices.contains(service)) {
        sortedServices.add(service);
      }
    }

    return sortedServices;
  }
}

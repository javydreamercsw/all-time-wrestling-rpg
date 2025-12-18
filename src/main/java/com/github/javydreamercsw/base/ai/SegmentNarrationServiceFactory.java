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

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the best available segment narration service. Provides automatic provider
 * selection based on availability and preference.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SegmentNarrationServiceFactory {

  private final List<SegmentNarrationService> availableServices;

  /**
   * Narrates a segment using the best available AI service with fallback.
   *
   * @param segmentContext The context for the segment narration.
   * @return The AI-generated segment narration.
   * @throws AIServiceException if all available services fail.
   */
  public String narrateSegment(
      @NonNull SegmentNarrationService.SegmentNarrationContext segmentContext) {
    List<SegmentNarrationService> services = getAvailableServicesInPriorityOrder();
    Exception lastException = null;

    for (SegmentNarrationService service : services) {
      try {
        log.debug("Attempting to narrate segment with provider: {}", service.getProviderName());
        return service.narrateSegment(segmentContext);
      } catch (AIServiceException e) {
        lastException = e;
        log.warn(
            "AI service provider {} failed to narrate segment: {}",
            service.getProviderName(),
            e.getMessage());
      }
    }
    throw new AIServiceException(
        503,
        "Service Unavailable",
        "All",
        "All AI providers failed to narrate the segment.",
        lastException);
  }

  /**
   * Summarizes a narration using the best available AI service with fallback.
   *
   * @param narration The narration to summarize.
   * @return The AI-generated summary.
   * @throws AIServiceException if all available services fail.
   */
  public String summarizeNarration(@NonNull String narration) {
    List<SegmentNarrationService> services = getAvailableServicesInPriorityOrder();
    Exception lastException = null;

    for (SegmentNarrationService service : services) {
      try {
        log.debug("Attempting to summarize narration with provider: {}", service.getProviderName());
        return service.summarizeNarration(narration);
      } catch (AIServiceException e) {
        lastException = e;
        log.warn(
            "AI service provider {} failed to summarize narration: {}",
            service.getProviderName(),
            e.getMessage());
      }
    }
    throw new AIServiceException(
        503,
        "Service Unavailable",
        "All",
        "All AI providers failed to summarize the narration.",
        lastException);
  }

  /**
   * Generates text using the best available AI service with fallback.
   *
   * @param prompt The prompt to generate text from.
   * @return The AI-generated text.
   * @throws AIServiceException if all available services fail.
   */
  public String generateText(@NonNull String prompt) {
    List<SegmentNarrationService> services = getAvailableServicesInPriorityOrder();
    Exception lastException = null;

    for (SegmentNarrationService service : services) {
      try {
        log.debug("Attempting to generate text with provider: {}", service.getProviderName());
        return service.generateText(prompt);
      } catch (AIServiceException e) {
        lastException = e;
        log.warn(
            "AI service provider {} failed to generate text: {}",
            service.getProviderName(),
            e.getMessage());
      }
    }
    throw new AIServiceException(
        503,
        "Service Unavailable",
        "All",
        "All AI providers failed to generate text.",
        lastException);
  }

  /**
   * Gets the best available segment narration service based on cost-effectiveness and quality.
   *
   * <p>Priority order (cost-effectiveness first): 1. Gemini (FREE tier + excellent quality) - Best
   * value 2. Claude Haiku (~$0.25/1K input, $1.25/1K output) - Good quality, reasonable cost 3.
   * OpenAI GPT-3.5 (~$0.50/1K input, $1.50/1K output) - Good quality, moderate cost 4. Claude
   * Sonnet (~$3/1K input, $15/1K output) - Excellent quality, expensive 5. OpenAI GPT-4 (~$10/1K
   * input, $30/1K output) - Excellent quality, very expensive 6. Mock (testing only)
   *
   * @return The best available service, or null if none are available
   */
  public SegmentNarrationService getBestAvailableService() {
    List<SegmentNarrationService> sortedServices = getAvailableServicesInPriorityOrder();
    if (!sortedServices.isEmpty()) {
      SegmentNarrationService service = sortedServices.get(0);
      log.debug(
          "Selected segment narration provider: {} (priority: {}, cost: ${}/1K tokens, reason: {})",
          service.getProviderName(),
          getCostInfo(service.getProviderName()).priority(),
          getCostInfo(service.getProviderName()).costPer1KTokens(),
          getCostInfo(service.getProviderName()).description());
      return service;
    }

    log.warn("No segment narration services are available");
    return null;
  }

  /**
   * Gets all available segment narration services in priority order (cost-effectiveness first).
   *
   * @return List of available services, sorted by priority.
   */
  public List<SegmentNarrationService> getAvailableServicesInPriorityOrder() {
    // Define priority order based on cost-effectiveness
    ProviderPriority[] priorityOrder = {
      new ProviderPriority("LocalAI", 1, 0.0, "Local AI, free and private"),
      new ProviderPriority("Gemini", 2, 0.0, "FREE tier with excellent quality"),
      new ProviderPriority("Claude", 3, 0.25, "Claude Haiku - good quality, reasonable cost"),
      new ProviderPriority("OpenAI", 4, 0.50, "GPT-3.5 - good quality, moderate cost"),
      new ProviderPriority("Mock", 10, 0.0, "Mock AI for testing and development")
    };

    List<SegmentNarrationService> sortedServices = new java.util.ArrayList<>();

    for (ProviderPriority priority : priorityOrder) {
      for (SegmentNarrationService service : availableServices) {
        if (service.getProviderName().toLowerCase().contains(priority.name().toLowerCase())
            && service.isAvailable()) {
          sortedServices.add(service);
        }
      }
    }

    // Add any other available services that were not explicitly prioritized
    for (SegmentNarrationService service : availableServices) {
      if (service.isAvailable() && !sortedServices.contains(service)) {
        sortedServices.add(service);
      }
    }

    return sortedServices;
  }

  /** Provider priority information for cost-based selection. */
  private record ProviderPriority(
      String name, int priority, double costPer1KTokens, String reason) {}

  /**
   * Gets a specific segment narration service by provider name.
   *
   * @param providerName The name of the provider (e.g., "Gemini", "Claude", "OpenAI", "Mock")
   * @return The service if available, null otherwise
   */
  public SegmentNarrationService getServiceByProvider(String providerName) {
    return availableServices.stream()
        .filter(
            service -> service.getProviderName().toLowerCase().contains(providerName.toLowerCase()))
        .filter(SegmentNarrationService::isAvailable)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets the mock service for testing purposes. The mock service is always available and generates
   * realistic narrations.
   *
   * @return The mock service, or null if not found
   */
  public SegmentNarrationService getMockService() {
    return getServiceByProvider("Mock");
  }

  /**
   * Forces the use of mock service for testing, even if real AI providers are available. Useful for
   * development and testing scenarios.
   *
   * @return The mock service
   */
  public SegmentNarrationService getTestingService() {
    SegmentNarrationService mockService = getMockService();
    if (mockService != null) {
      log.info("Using Mock AI service for testing (forced selection)");
      return mockService;
    }

    log.warn("Mock service not available, falling back to best available service");
    return getBestAvailableService();
  }

  /**
   * Gets information about all available segment narration services with cost details.
   *
   * @return List of service information with pricing
   */
  public List<ServiceInfo> getAvailableServices() {
    return availableServices.stream()
        .map(
            service -> {
              CostInfo costInfo = getCostInfo(service.getProviderName());
              return new ServiceInfo(
                  service.getProviderName(),
                  service.isAvailable(),
                  service.getClass().getSimpleName(),
                  costInfo.priority(),
                  costInfo.costPer1KTokens(),
                  costInfo.tier(),
                  costInfo.description());
            })
        .toList();
  }

  /** Gets cost and priority information for a provider. */
  private CostInfo getCostInfo(String providerName) {
    String lowerName = providerName.toLowerCase();

    if (lowerName.contains("localai")) {
      return new CostInfo(0, 0.0, "FREE", "Local AI, free and private");
    } else if (lowerName.contains("gemini")) {
      return new CostInfo(1, 0.0, "FREE", "Free tier: 15 req/min, 1.5K req/day, excellent quality");
    } else if (lowerName.contains("claude")) {
      return new CostInfo(
          2,
          0.25,
          "PAID",
          "Claude Haiku: $0.25/1K input, $1.25/1K output - good quality, reasonable cost");
    } else if (lowerName.contains("openai")) {
      if (lowerName.contains("gpt-4")) {
        return new CostInfo(
            5,
            10.0,
            "PREMIUM",
            "GPT-4: $10/1K input, $30/1K output - excellent quality, expensive");
      } else {
        return new CostInfo(
            3,
            0.50,
            "PAID",
            "GPT-3.5: $0.50/1K input, $1.50/1K output - good quality, moderate cost");
      }
    } else if (lowerName.contains("mock")) {
      return new CostInfo(
          10,
          0.0,
          "FREE",
          "Mock AI: Always available, realistic narrations, perfect for testing and development");
    } else {
      return new CostInfo(50, 999.0, "UNKNOWN", "Unknown provider pricing");
    }
  }

  /**
   * Gets the estimated cost for a typical segment narration.
   *
   * @param providerName The provider name
   * @return Estimated cost in USD
   */
  public double getEstimatedSegmentCost(String providerName) {
    CostInfo costInfo = getCostInfo(providerName);

    // Typical segment narration:
    // Input: ~2000 tokens (rich context)
    // Output: ~3000 tokens (detailed narration)
    double inputTokens = 2.0; // 2K tokens
    double outputTokens = 3.0; // 3K tokens

    if (costInfo.costPer1KTokens() == 0.0) {
      return 0.0; // Free tier
    }

    // Calculate costs based on provider-specific pricing models
    double inputCost = inputTokens * costInfo.costPer1KTokens();
    double outputCost;

    if (providerName.toLowerCase().contains("gpt-4")) {
      // GPT-4: $10 input, $30 output (3x multiplier)
      outputCost = outputTokens * (costInfo.costPer1KTokens() * 3);
    } else if (providerName.toLowerCase().contains("openai")) {
      // GPT-3.5: $0.50 input, $1.50 output (3x multiplier)
      outputCost = outputTokens * (costInfo.costPer1KTokens() * 3);
    } else if (providerName.toLowerCase().contains("claude")) {
      // Claude: $0.25 input, $1.25 output (5x multiplier)
      outputCost = outputTokens * (costInfo.costPer1KTokens() * 5);
    } else {
      // Default: 5x multiplier for other providers
      outputCost = outputTokens * (costInfo.costPer1KTokens() * 5);
    }

    return inputCost + outputCost;
  }

  /** Information about a segment narration service with cost details. */
  public record ServiceInfo(
      String providerName,
      boolean available,
      String className,
      int priority,
      double costPer1KTokens,
      String tier,
      String description) {}

  /** Cost information for a provider. */
  private record CostInfo(int priority, double costPer1KTokens, String tier, String description) {}
}

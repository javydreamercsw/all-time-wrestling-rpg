package com.github.javydreamercsw.base.ai;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the best available match narration service. Provides automatic provider
 * selection based on availability and preference.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchNarrationServiceFactory {

  private final List<MatchNarrationService> availableServices;

  /**
   * Gets the best available match narration service based on cost-effectiveness and quality.
   *
   * <p>Priority order (cost-effectiveness first): 1. Gemini (FREE tier + excellent quality) - Best
   * value 2. Claude Haiku (~$0.25/1K input, $1.25/1K output) - Good quality, reasonable cost 3.
   * OpenAI GPT-3.5 (~$0.50/1K input, $1.50/1K output) - Good quality, moderate cost 4. Claude
   * Sonnet (~$3/1K input, $15/1K output) - Excellent quality, expensive 5. OpenAI GPT-4 (~$10/1K
   * input, $30/1K output) - Excellent quality, very expensive 6. Mock (testing only)
   *
   * @return The best available service, or null if none are available
   */
  public MatchNarrationService getBestAvailableService() {
    // Define priority order based on cost-effectiveness
    ProviderPriority[] priorityOrder = {
      new ProviderPriority("Gemini", 1, 0.0, "FREE tier with excellent quality"),
      new ProviderPriority("Claude", 2, 0.25, "Claude Haiku - good quality, reasonable cost"),
      new ProviderPriority("OpenAI", 3, 0.50, "GPT-3.5 - good quality, moderate cost"),
      new ProviderPriority("Mock", 99, 0.0, "Testing only")
    };

    for (ProviderPriority priority : priorityOrder) {
      for (MatchNarrationService service : availableServices) {
        if (service.getProviderName().toLowerCase().contains(priority.name().toLowerCase())
            && service.isAvailable()) {
          log.info(
              "Selected match narration provider: {} (priority: {}, cost: ${}/1K tokens, reason:"
                  + " {})",
              service.getProviderName(),
              priority.priority(),
              priority.costPer1KTokens(),
              priority.reason());
          return service;
        }
      }
    }

    // Fallback: select any available service
    for (MatchNarrationService service : availableServices) {
      if (service.isAvailable()) {
        log.info(
            "Selected match narration provider: {} (fallback - no priority match)",
            service.getProviderName());
        return service;
      }
    }

    log.warn("No match narration services are available");
    return null;
  }

  /** Provider priority information for cost-based selection. */
  private record ProviderPriority(
      String name, int priority, double costPer1KTokens, String reason) {}

  /**
   * Gets a specific match narration service by provider name.
   *
   * @param providerName The name of the provider (e.g., "Gemini", "Claude", "OpenAI")
   * @return The service if available, null otherwise
   */
  public MatchNarrationService getServiceByProvider(String providerName) {
    return availableServices.stream()
        .filter(
            service -> service.getProviderName().toLowerCase().contains(providerName.toLowerCase()))
        .filter(MatchNarrationService::isAvailable)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets information about all available match narration services with cost details.
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

    if (lowerName.contains("gemini")) {
      return new CostInfo(1, 0.0, "FREE", "Free tier: 15 req/min, 1.5K req/day, excellent quality");
    } else if (lowerName.contains("claude")) {
      return new CostInfo(
          2,
          0.25,
          "PAID",
          "Claude Haiku: $0.25/1K input, $1.25/1K output - good quality, reasonable cost");
    } else if (lowerName.contains("openai")) {
      return new CostInfo(
          3,
          0.50,
          "PAID",
          "GPT-3.5: $0.50/1K input, $1.50/1K output - good quality, moderate cost");
    } else if (lowerName.contains("mock")) {
      return new CostInfo(99, 0.0, "FREE", "Mock service for testing - no real AI");
    } else {
      return new CostInfo(50, 999.0, "UNKNOWN", "Unknown provider pricing");
    }
  }

  /**
   * Gets the estimated cost for a typical match narration.
   *
   * @param providerName The provider name
   * @return Estimated cost in USD
   */
  public double getEstimatedMatchCost(String providerName) {
    CostInfo costInfo = getCostInfo(providerName);

    // Typical match narration:
    // Input: ~2000 tokens (rich context)
    // Output: ~3000 tokens (detailed narration)
    double inputTokens = 2.0; // 2K tokens
    double outputTokens = 3.0; // 3K tokens

    if (costInfo.costPer1KTokens() == 0.0) {
      return 0.0; // Free tier
    }

    // Estimate output cost as 5x input cost (typical AI pricing model)
    double inputCost = inputTokens * costInfo.costPer1KTokens();
    double outputCost = outputTokens * (costInfo.costPer1KTokens() * 5);

    return inputCost + outputCost;
  }

  /** Information about a match narration service with cost details. */
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

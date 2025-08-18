package com.github.javydreamercsw.base.ai;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory class for managing AI service providers. Automatically selects the best available AI
 * service based on configuration and availability.
 */
@Component
@Slf4j
public class AIServiceFactory {

  @Value("${ai.provider:auto}")
  private String preferredProvider;

  @Autowired private List<AIService> availableServices;

  /**
   * Gets the configured AI service instance.
   *
   * @return The AI service to use, or null if none available
   */
  public AIService getAIService() {
    log.debug("Selecting AI service with preferred provider: {}", preferredProvider);

    // If a specific provider is requested, try to find it
    if (!"auto".equalsIgnoreCase(preferredProvider)) {
      for (AIService service : availableServices) {
        if (service.getProviderName().toLowerCase().contains(preferredProvider.toLowerCase())
            && service.isAvailable()) {
          log.info("Using configured AI provider: {}", service.getProviderName());
          return service;
        }
      }
      log.warn(
          "Requested AI provider '{}' not available, falling back to auto-selection",
          preferredProvider);
    }

    // Auto-select based on preferred order: Gemini (free) > OpenAI > Claude > Mock
    String[] preferredOrder = {"Gemini", "OpenAI", "Claude", "Mock"};

    for (String preferredName : preferredOrder) {
      for (AIService service : availableServices) {
        if (service.getProviderName().contains(preferredName) && service.isAvailable()) {
          log.info("Auto-selected AI provider: {} (preferred order)", service.getProviderName());
          return service;
        }
      }
    }

    // Fallback: select any available service
    for (AIService service : availableServices) {
      if (service.isAvailable()) {
        log.info("Auto-selected AI provider: {} (fallback)", service.getProviderName());
        return service;
      }
    }

    log.warn("No AI services are available. Please configure an AI provider.");
    return null;
  }

  /**
   * Gets information about all available AI services.
   *
   * @return List of service info strings
   */
  public List<String> getAvailableServices() {
    return availableServices.stream()
        .map(
            service ->
                String.format(
                    "%s (Available: %s)", service.getProviderName(), service.isAvailable()))
        .toList();
  }

  /**
   * Checks if any AI service is available.
   *
   * @return true if at least one AI service is configured and available
   */
  public boolean isAnyServiceAvailable() {
    return availableServices.stream().anyMatch(AIService::isAvailable);
  }

  /**
   * Gets the name of the currently selected AI provider.
   *
   * @return Provider name or "None" if no service available
   */
  public String getCurrentProviderName() {
    AIService service = getAIService();
    return service != null ? service.getProviderName() : "None";
  }
}

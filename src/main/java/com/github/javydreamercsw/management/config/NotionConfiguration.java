package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for Notion-related beans. Only active when Notion sync is enabled. */
@Configuration
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class NotionConfiguration {

  /**
   * Provides the NotionHandler singleton as a Spring bean. This allows the NotionHandler to be
   * injected into other Spring components.
   *
   * <p>For integration tests, this will attempt to create the NotionHandler but will handle
   * authentication failures gracefully to allow the Spring context to load.
   *
   * @return NotionHandler singleton instance, or null if initialization fails
   */
  @Bean
  public NotionHandler notionHandler() {
    try {
      return NotionHandler.getInstance();
    } catch (Exception e) {
      log.warn(
          "Failed to initialize NotionHandler (this is expected in tests with invalid tokens): {}",
          e.getMessage());
      // For integration tests, we still want the Spring context to load
      // The actual sync operations will handle the missing handler gracefully
      return null;
    }
  }
}

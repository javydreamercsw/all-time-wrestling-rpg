package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for Notion-related beans. Only active when Notion sync is enabled. */
@Configuration
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
public class NotionConfiguration {

  /**
   * Provides the NotionHandler singleton as a Spring bean. This allows the NotionHandler to be
   * injected into other Spring components.
   *
   * @return NotionHandler singleton instance
   */
  @Bean
  public NotionHandler notionHandler() {
    return NotionHandler.getInstance();
  }
}

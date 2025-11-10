package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Configuration class for Notion-related beans. Only active when Notion sync is enabled. */
@Configuration
@Slf4j
@Profile("!test")
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
  @ConditionalOnProperty(name = "NOTION_TOKEN")
  public NotionHandler notionHandler() {
    return NotionHandler.getInstance().orElse(null);
  }
}

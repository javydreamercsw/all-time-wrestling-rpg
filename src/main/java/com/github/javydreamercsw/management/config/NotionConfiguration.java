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

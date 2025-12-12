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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.entity.notion.NotionSyncServiceDependencies;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestNotionConfiguration {

  @Bean
  @Primary
  public NotionHandler testNotionHandler() {
    return mock(NotionHandler.class);
  }

  @Bean
  @Primary
  public NotionRateLimitService testNotionRateLimitService() {
    return mock(NotionRateLimitService.class);
  }

  @Bean
  @Primary
  public NotionSyncProperties testNotionSyncProperties() {
    NotionSyncProperties mockProperties = mock(NotionSyncProperties.class);
    when(mockProperties.getParallelThreads()).thenReturn(1);
    return mockProperties;
  }

  @Bean
  @Primary
  public NotionSyncServiceDependencies testNotionSyncServiceDependencies(
      NotionHandler notionHandler,
      NotionRateLimitService notionRateLimitService,
      NotionSyncProperties notionSyncProperties) {
    NotionSyncServiceDependencies mockDependencies = mock(NotionSyncServiceDependencies.class);
    when(mockDependencies.getNotionHandler()).thenReturn(notionHandler);
    when(mockDependencies.getNotionRateLimitService()).thenReturn(notionRateLimitService);
    when(mockDependencies.getNotionSyncProperties()).thenReturn(notionSyncProperties);
    return mockDependencies;
  }

  @Bean
  @Primary
  public NotionApiExecutor testNotionApiExecutor(
      NotionHandler notionHandler,
      NotionRateLimitService notionRateLimitService,
      NotionSyncProperties notionSyncProperties) {
    return new NotionApiExecutor(notionHandler, notionRateLimitService, notionSyncProperties);
  }
}

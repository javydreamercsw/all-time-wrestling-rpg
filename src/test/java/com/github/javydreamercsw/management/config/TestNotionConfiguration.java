package com.github.javydreamercsw.management.config;

import static org.mockito.Mockito.mock;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestNotionConfiguration {

  @Bean
  @Primary
  public NotionHandler notionHandler() {
    return mock(NotionHandler.class);
  }
}

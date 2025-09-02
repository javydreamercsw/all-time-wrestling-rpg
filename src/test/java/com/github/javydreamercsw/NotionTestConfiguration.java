package com.github.javydreamercsw;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class NotionTestConfiguration {

  @Bean
  public NotionHandler notionHandler() {
    return NotionHandler.getInstance();
  }
}

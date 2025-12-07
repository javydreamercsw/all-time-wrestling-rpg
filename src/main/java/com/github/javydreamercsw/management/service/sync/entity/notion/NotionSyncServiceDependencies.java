package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.notion.NotionHandler;
import com.github.javydreamercsw.base.notion.NotionRateLimitService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NotionSyncServiceDependencies {
  private final NotionHandler notionHandler;
  private final NotionRateLimitService notionRateLimitService;
  private final NotionSyncProperties notionSyncProperties;

  @Autowired
  public NotionSyncServiceDependencies(
      NotionHandler notionHandler,
      NotionRateLimitService notionRateLimitService,
      NotionSyncProperties notionSyncProperties) {
    this.notionHandler = notionHandler;
    this.notionRateLimitService = notionRateLimitService;
    this.notionSyncProperties = notionSyncProperties;
  }
}

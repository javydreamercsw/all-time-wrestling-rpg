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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
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

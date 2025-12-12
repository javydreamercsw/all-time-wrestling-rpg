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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SyncServiceDependencies {
  private final SyncProgressTracker progressTracker;
  private final ISyncHealthMonitor healthMonitor;
  private final RetryService retryService;
  private final CircuitBreakerService circuitBreakerService;
  private final SyncValidationService validationService;
  private final SyncTransactionManager syncTransactionManager;
  private final DataIntegrityChecker integrityChecker;
  private final NotionRateLimitService rateLimitService;
  private final NotionSyncProperties notionSyncProperties; // New field
  private final EntitySyncConfiguration entitySyncConfig;
  private final NotionHandler notionHandler;
  private final NotionPageDataExtractor notionPageDataExtractor;
  private final SyncSessionManager syncSessionManager;
  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;
  private final InjuryTypeRepository injuryTypeRepository;
  private final ShowTypeRepository showTypeRepository;
  private final TeamRepository teamRepository;
  private final TitleReignRepository titleReignRepository;
  private final TitleRepository titleRepository;

  @Autowired
  public SyncServiceDependencies(
      @NonNull SyncProgressTracker progressTracker,
      @NonNull ISyncHealthMonitor healthMonitor,
      @NonNull RetryService retryService,
      @NonNull CircuitBreakerService circuitBreakerService,
      @NonNull SyncValidationService validationService,
      @NonNull SyncTransactionManager syncTransactionManager,
      @NonNull DataIntegrityChecker integrityChecker,
      @NonNull NotionRateLimitService rateLimitService,
      @NonNull EntitySyncConfiguration entitySyncConfig,
      @NonNull NotionSyncProperties notionSyncProperties, // New parameter
      @NonNull NotionHandler notionHandler,
      @NonNull NotionPageDataExtractor notionPageDataExtractor,
      @NonNull SyncSessionManager syncSessionManager,
      @NonNull FactionRepository factionRepository,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull InjuryTypeRepository injuryTypeRepository,
      @NonNull ShowTypeRepository showTypeRepository,
      @NonNull TeamRepository teamRepository,
      @NonNull TitleReignRepository titleReignRepository,
      @NonNull TitleRepository titleRepository) {
    this.progressTracker = progressTracker;
    this.healthMonitor = healthMonitor;
    this.retryService = retryService;
    this.circuitBreakerService = circuitBreakerService;
    this.validationService = validationService;
    this.syncTransactionManager = syncTransactionManager;
    this.integrityChecker = integrityChecker;
    this.rateLimitService = rateLimitService;
    this.entitySyncConfig = entitySyncConfig;
    this.notionSyncProperties = notionSyncProperties; // Initialize new field
    this.notionHandler = notionHandler;
    this.notionPageDataExtractor = notionPageDataExtractor;
    this.syncSessionManager = syncSessionManager;
    this.factionRepository = factionRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.injuryTypeRepository = injuryTypeRepository;
    this.showTypeRepository = showTypeRepository;
    this.teamRepository = teamRepository;
    this.titleReignRepository = titleReignRepository;
    this.titleRepository = titleRepository;
  }
}

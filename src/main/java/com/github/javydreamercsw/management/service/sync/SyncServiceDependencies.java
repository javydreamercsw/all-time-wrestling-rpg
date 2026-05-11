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
import com.github.javydreamercsw.base.config.StorageProperties;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.lock.SyncLockService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
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
  private final NotionSyncProperties notionSyncProperties;
  private final StorageProperties storageProperties;
  private final ResourceLoader resourceLoader;
  private final BackupService backupService;
  private final EntitySyncConfiguration entitySyncConfig;
  private final NotionHandler notionHandler;
  private final NotionPageDataExtractor notionPageDataExtractor;
  private final SyncSessionManager syncSessionManager;
  private final SyncLockService syncLockService;
  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final InjuryRepository injuryRepository;
  private final InjuryTypeRepository injuryTypeRepository;
  private final SeasonRepository seasonRepository;
  private final ShowRepository showRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final ShowTypeRepository showTypeRepository;
  private final TeamRepository teamRepository;
  private final TitleReignRepository titleReignRepository;
  private final TitleRepository titleRepository;
  private final NpcRepository npcRepository;
  private final RivalryRepository rivalryRepository;
  private final SegmentRepository segmentRepository;

  @Autowired
  public SyncServiceDependencies(
      @NonNull final SyncProgressTracker progressTracker,
      @NonNull final ISyncHealthMonitor healthMonitor,
      @NonNull final RetryService retryService,
      @NonNull final CircuitBreakerService circuitBreakerService,
      @NonNull final SyncValidationService validationService,
      @NonNull final SyncTransactionManager syncTransactionManager,
      @NonNull final DataIntegrityChecker integrityChecker,
      @NonNull final NotionRateLimitService rateLimitService,
      @NonNull final EntitySyncConfiguration entitySyncConfig,
      @NonNull final NotionSyncProperties notionSyncProperties,
      @NonNull final StorageProperties storageProperties,
      @NonNull final ResourceLoader resourceLoader,
      @NonNull final BackupService backupService,
      @NonNull final NotionHandler notionHandler,
      @NonNull final NotionPageDataExtractor notionPageDataExtractor,
      @NonNull final SyncSessionManager syncSessionManager,
      @NonNull final SyncLockService syncLockService,
      @NonNull final FactionRepository factionRepository,
      @NonNull final WrestlerRepository wrestlerRepository,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final InjuryRepository injuryRepository,
      @NonNull final InjuryTypeRepository injuryTypeRepository,
      @NonNull final SeasonRepository seasonRepository,
      @NonNull final RivalryRepository rivalryRepository,
      @NonNull final ShowRepository showRepository,
      @NonNull final ShowTemplateRepository showTemplateRepository,
      @NonNull final ShowTypeRepository showTypeRepository,
      @NonNull final TeamRepository teamRepository,
      @NonNull final TitleReignRepository titleReignRepository,
      @NonNull final TitleRepository titleRepository,
      @NonNull final NpcRepository npcRepository,
      @NonNull final SegmentRepository segmentRepository) {
    this.progressTracker = progressTracker;
    this.healthMonitor = healthMonitor;
    this.retryService = retryService;
    this.circuitBreakerService = circuitBreakerService;
    this.validationService = validationService;
    this.syncTransactionManager = syncTransactionManager;
    this.integrityChecker = integrityChecker;
    this.rateLimitService = rateLimitService;
    this.entitySyncConfig = entitySyncConfig;
    this.notionSyncProperties = notionSyncProperties;
    this.storageProperties = storageProperties;
    this.resourceLoader = resourceLoader;
    this.backupService = backupService;
    this.notionHandler = notionHandler;
    this.notionPageDataExtractor = notionPageDataExtractor;
    this.syncSessionManager = syncSessionManager;
    this.syncLockService = syncLockService;
    this.factionRepository = factionRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.injuryRepository = injuryRepository;
    this.injuryTypeRepository = injuryTypeRepository;
    this.seasonRepository = seasonRepository;
    this.rivalryRepository = rivalryRepository;
    this.showRepository = showRepository;
    this.showTemplateRepository = showTemplateRepository;
    this.showTypeRepository = showTypeRepository;
    this.teamRepository = teamRepository;
    this.titleReignRepository = titleReignRepository;
    this.titleRepository = titleRepository;
    this.npcRepository = npcRepository;
    this.segmentRepository = segmentRepository;
  }
}

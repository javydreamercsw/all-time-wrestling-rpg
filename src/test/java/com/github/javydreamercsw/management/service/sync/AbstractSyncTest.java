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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractSyncTest {

  @Mock protected NotionHandler notionHandler;
  @Mock protected NotionSyncProperties syncProperties;
  @Mock protected SyncProgressTracker progressTracker;
  @Mock protected SyncHealthMonitor healthMonitor;
  @Mock protected ObjectMapper objectMapper;
  @Mock protected NotionRateLimitService rateLimitService;
  @Mock protected SyncSessionManager syncSessionManager;
  protected SyncServiceDependencies syncServiceDependencies;
  @Mock protected NotionPageDataExtractor notionPageDataExtractor;
  @Mock protected NotionApiExecutor notionApiExecutor;
  @Mock protected RetryService retryService;
  @Mock protected CircuitBreakerService circuitBreakerService;
  @Mock protected SyncValidationService validationService;
  @Mock protected SyncTransactionManager syncTransactionManager;
  @Mock protected DataIntegrityChecker integrityChecker;
  @Mock protected EntitySyncConfiguration entitySyncConfig;
  @Mock protected FactionRepository factionRepository;
  @Mock protected WrestlerRepository wrestlerRepository;
  @Mock protected InjuryTypeRepository injuryTypeRepository;
  @Mock protected ShowTypeRepository showTypeRepository;
  @Mock protected TeamRepository teamRepository;
  @Mock protected TitleReignRepository titleReignRepository;
  @Mock protected TitleRepository titleRepository;

  protected static MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeAll
  static void beforeAll() {
    mockedEnvironmentVariableUtil = Mockito.mockStatic(EnvironmentVariableUtil.class);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::getNotionToken)
        .thenReturn("test-token");
  }

  @AfterAll
  static void afterAll() {
    mockedEnvironmentVariableUtil.close();
  }

  @BeforeEach
  protected void setUp() {
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
    lenient()
        .when(notionApiExecutor.getSyncExecutorService())
        .thenReturn(java.util.concurrent.Executors.newSingleThreadExecutor());

    syncServiceDependencies =
        new SyncServiceDependencies(
            progressTracker,
            healthMonitor,
            retryService,
            circuitBreakerService,
            validationService,
            syncTransactionManager,
            integrityChecker,
            rateLimitService,
            entitySyncConfig,
            syncProperties,
            notionHandler,
            notionPageDataExtractor,
            syncSessionManager,
            factionRepository,
            wrestlerRepository,
            injuryTypeRepository,
            showTypeRepository,
            teamRepository,
            titleReignRepository,
            titleRepository);
  }
}

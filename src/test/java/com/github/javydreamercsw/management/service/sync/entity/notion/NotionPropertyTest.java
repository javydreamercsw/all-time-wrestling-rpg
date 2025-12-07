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

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test to verify Notion property resolution fixes by mocking dependencies. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notion Property Unit Test")
class NotionPropertyTest {

  @Mock private SegmentSyncService segmentSyncService;
  @Mock private SegmentNotionSyncService segmentNotionSyncService;
  @Mock private ParallelSyncOrchestrator parallelSyncOrchestrator;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties notionSyncProperties;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncServiceDependencies syncServiceDependencies;
  @Mock private NotionSyncServiceDependencies notionSyncServiceDependencies;

  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  private MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Mock static method EnvironmentVariableUtil.isNotionTokenAvailable()
    mockedEnvironmentVariableUtil = mockStatic(EnvironmentVariableUtil.class);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);

    // Configure mock behavior for NotionSyncProperties
    when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(notionSyncProperties);
    when(notionSyncProperties.getParallelThreads()).thenReturn(1);
    when(notionSyncServiceDependencies.getSegmentSyncService()).thenReturn(segmentSyncService);
    when(notionSyncServiceDependencies.getSegmentNotionSyncService())
        .thenReturn(segmentNotionSyncService);

    notionSyncService =
        new NotionSyncService(
            objectMapper,
            syncServiceDependencies,
            notionSyncServiceDependencies,
            parallelSyncOrchestrator) {
          @Override
          public BaseSyncService.SyncResult syncShow(@NonNull String showId) {
            return notionSyncServiceDependencies.getShowSyncService().syncShow(showId);
          }

          @Override
          public List<String> getAllShowIds() {
            return notionSyncServiceDependencies.getShowSyncService().getShowIds();
          }

          @Override
          public BaseSyncService.SyncResult syncSegment(@NonNull String segmentId) {
            return notionSyncServiceDependencies.getSegmentSyncService().syncSegment(segmentId);
          }

          @Override
          public BaseSyncService.SyncResult syncShows(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getShowSyncService().syncShows(operationId)
                : notionSyncServiceDependencies
                    .getShowNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncWrestlers(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getWrestlerSyncService().syncWrestlers(operationId)
                : notionSyncServiceDependencies
                    .getWrestlerNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncFactions(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getFactionSyncService().syncFactions(operationId)
                : notionSyncServiceDependencies
                    .getFactionNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncTeams(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getTeamSyncService().syncTeams(operationId)
                : notionSyncServiceDependencies
                    .getTeamNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncShowTemplates(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies
                    .getShowTemplateSyncService()
                    .syncShowTemplates(operationId)
                : notionSyncServiceDependencies
                    .getShowTemplateNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncSeasons(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getSeasonSyncService().syncSeasons(operationId)
                : notionSyncServiceDependencies
                    .getSeasonNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncShowTypes(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getShowTypeSyncService().syncShowTypes(operationId)
                : notionSyncServiceDependencies
                    .getShowTypeNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncInjuryTypes(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getInjurySyncService().syncInjuryTypes(operationId)
                : notionSyncServiceDependencies
                    .getInjuryNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncNpcs(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getNpcSyncService().syncNpcs(operationId, direction)
                : notionSyncServiceDependencies.getNpcNotionSyncService().syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncTitles(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getTitleSyncService().syncTitles(operationId)
                : notionSyncServiceDependencies
                    .getTitleNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncTitleReigns(@NonNull String operationId) {
            return notionSyncServiceDependencies
                .getTitleReignSyncService()
                .syncTitleReigns(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncRivalries(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies.getRivalrySyncService().syncRivalries(operationId)
                : notionSyncServiceDependencies
                    .getRivalryNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncFactionRivalries(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return direction.equals(SyncDirection.INBOUND)
                ? notionSyncServiceDependencies
                    .getFactionRivalrySyncService()
                    .syncFactionRivalries(operationId)
                : notionSyncServiceDependencies
                    .getFactionRivalryNotionSyncService()
                    .syncToNotion(operationId);
          }

          @Override
          public BaseSyncService.SyncResult syncSegments(
              @NonNull String operationId, @NonNull SyncDirection direction) {
            return notionSyncServiceDependencies
                .getSegmentNotionSyncService()
                .syncToNotion(operationId);
          }
        };
  }
}

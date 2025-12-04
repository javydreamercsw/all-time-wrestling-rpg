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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Show Type Sync Service Tests")
class ShowTypeSyncServiceTest extends AbstractSyncTest {

  @Mock private ShowTypeService showTypeService;

  private ShowTypeSyncService showTypeSyncService;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Call parent setup first
    showTypeSyncService = new ShowTypeSyncService(objectMapper, syncProperties, notionHandler);
    ReflectionTestUtils.setField(showTypeSyncService, "showTypeService", showTypeService);
    ReflectionTestUtils.setField(showTypeSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(showTypeSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(showTypeSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  @DisplayName("Should extract show types from Notion and create them in database")
  void shouldExtractAndCreateShowTypesFromNotion() {
    // Given
    List<ShowPage> mockShowPages = createMockShowPages();
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    lenient().when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.empty());
    lenient().when(showTypeService.findAll()).thenReturn(List.of());
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes("test-operation-id");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Show Types");
    assertThat(result.getSyncedCount()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should ensure default show types exist in database")
  void shouldEnsureDefaultShowTypesExist() {
    // Given - Initially empty database
    lenient().when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(Collections.emptyList());
    lenient().when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When - Sync show types (should create defaults when no Notion data available)
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes("test-operation-id");

    // Then - Should have created default show types
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    verify(showTypeService, times(2)).save(any(ShowType.class));
  }

  private List<ShowPage> createMockShowPages() {
    List<ShowPage> showPages = new ArrayList<>();

    ShowPage weeklyShow = new ShowPage();
    Map<String, Object> weeklyProps = new HashMap<>();
    weeklyProps.put("Show Type", "Weekly");
    ReflectionTestUtils.setField(weeklyShow, "rawProperties", weeklyProps);
    showPages.add(weeklyShow);

    ShowPage pleShow = new ShowPage();
    Map<String, Object> pleProps = new HashMap<>();
    pleProps.put("Show Type", "Premium Live Event (PLE)");
    ReflectionTestUtils.setField(pleShow, "rawProperties", pleProps);
    showPages.add(pleShow);

    ShowPage anotherWeeklyShow = new ShowPage();
    Map<String, Object> anotherWeeklyProps = new HashMap<>();
    anotherWeeklyProps.put("Show Type", "Weekly");
    ReflectionTestUtils.setField(anotherWeeklyShow, "rawProperties", anotherWeeklyProps);
    showPages.add(anotherWeeklyShow);
    return showPages;
  }

  private List<ShowType> createMockShowTypes() {
    ShowType weekly = new ShowType();
    weekly.setName("Weekly");
    weekly.setDescription("Weekly show type");

    ShowType ple = new ShowType();
    ple.setName("Premium Live Event (PLE)");
    ple.setDescription("Premium Live Event show type");

    return List.of(weekly, ple);
  }

  @Test
  @DisplayName("Should not duplicate show types on subsequent syncs")
  void shouldNotDuplicateShowTypesOnSubsequentSyncs() {
    // Given - Initial state with some show types
    List<ShowType> initialShowTypes = createMockShowTypes();
    lenient().when(showTypeService.findAll()).thenReturn(initialShowTypes);
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(createMockShowPages());
    lenient()
        .when(showTypeService.findByName("Weekly"))
        .thenReturn(Optional.of(initialShowTypes.get(0)));
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.of(initialShowTypes.get(1)));
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When - Run first sync
    BaseSyncService.SyncResult firstResult =
        showTypeSyncService.syncShowTypes("test-operation-id-1");

    // Then - Should not create duplicates
    assertThat(firstResult).isNotNull();
    assertThat(firstResult.isSuccess()).isTrue();
    assertThat(firstResult.getSyncedCount()).isEqualTo(2); // Two updates

    // When - Run second sync
    BaseSyncService.SyncResult secondResult =
        showTypeSyncService.syncShowTypes("test-operation-id-1");

    // Then - Still no duplicates, and the second sync should be skipped
    assertThat(secondResult).isNotNull();
    assertThat(secondResult.isSuccess()).isTrue();
    assertThat(secondResult.getSyncedCount())
        .isZero(); // Because it's already synced in this session

    verify(showTypeService, times(2))
        .save(any(ShowType.class)); // Save is only called for the first sync
  }

  @Test
  @DisplayName("Should handle sync when show types already exist")
  void shouldHandleSyncWhenShowTypesAlreadyExist() {
    // Given - Manually create a show type
    ShowType existingType = new ShowType();
    existingType.setName("Weekly");
    existingType.setDescription("Pre-existing weekly show type");
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(existingType));
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.findAll())
        .thenReturn(List.of(existingType)); // Mock findAll to prevent default saves
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(createMockShowPages());

    // When - Run sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes("test-operation-id");

    // Then - Should not overwrite existing show type, and only save the new one
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(2); // One update, one creation

    verify(showTypeService, times(2))
        .save(any(ShowType.class)); // One for existingType, one for PLE

    verify(showTypeService, times(2)).findByName(anyString()); // findByName for Weekly and PLE
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {

    // Given - NotionHandler throws an exception

    lenient()
        .when(notionHandler.loadAllShowsForSync())
        .thenThrow(new RuntimeException("Notion API error"));

    lenient()
        .when(showTypeService.findAll())
        .thenReturn(Collections.emptyList()); // Ensure default types are created

    lenient().when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.empty());

    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When - Attempt sync

    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes("test-operation-id");

    // Then - Should report failure gracefully

    assertThat(result).isNotNull();

    assertThat(result.isSuccess()).isFalse();

    assertThat(result.getEntityType()).isEqualTo("Show Types");

    assertThat(result.getErrorMessage()).contains("Notion API error");

    // Verify that the health monitor was updated with the failure
    verify(healthMonitor, times(1)).recordFailure(eq("Show Types"), anyString());

    // Verify that no show types were saved
    verify(showTypeService, never()).save(any(ShowType.class));
  }

  @Test
  @DisplayName("Should track sync progress correctly")
  void shouldTrackSyncProgressCorrectly() {
    // Given
    String operationId = "test-operation-id-progress";
    List<ShowPage> mockShowPages = createMockShowPages();
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    lenient().when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(showTypeService.findByName(anyString())).thenReturn(Optional.empty());
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When - Run sync with operation ID for progress tracking
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(operationId);

    // Then - Should complete operation tracking
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isGreaterThan(0);
    verify(progressTracker).startOperation(operationId, "Sync Show Types", 4);
    verify(progressTracker, atLeastOnce()).updateProgress(eq(operationId), anyInt(), anyString());
    verify(progressTracker)
        .completeOperation(
            operationId,
            result.isSuccess(),
            String.format("Successfully synced %d show types (%d created, %d updated)", 2, 2, 0),
            2);
  }

  @Test
  @DisplayName("Should sync show types from Notion when token is available")
  void shouldSyncShowTypesFromNotionWhenTokenAvailable() {
    // Given
    List<ShowPage> mockShowPages = createMockShowPages();
    List<ShowType> existingShowTypes = createMockShowTypes();
    lenient().when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    lenient()
        .when(showTypeService.findAll())
        .thenReturn(existingShowTypes); // Return existing types to prevent default saves
    lenient()
        .when(showTypeService.findByName("Weekly"))
        .thenReturn(Optional.of(existingShowTypes.get(0)));
    lenient()
        .when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.of(existingShowTypes.get(1)));
    lenient()
        .when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When - Sync with mocked Notion connection
    BaseSyncService.SyncResult result =
        showTypeSyncService.syncShowTypes("test-operation-id-notion");

    // Then - Should attempt real sync
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(2); // Two updates

    verify(notionHandler).loadAllShowsForSync();
    verify(showTypeService, times(2)).save(any(ShowType.class)); // Weekly and PLE are updated
  }
}

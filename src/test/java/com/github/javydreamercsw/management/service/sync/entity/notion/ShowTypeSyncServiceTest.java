package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Show Type Sync Service Tests")
class ShowTypeSyncServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private ShowTypeService showTypeService;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock public NotionRateLimitService rateLimitService;

  private ShowTypeSyncService showTypeSyncService;

  @BeforeEach
  void setUp() {
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);

    showTypeSyncService = new ShowTypeSyncService(objectMapper, syncProperties, notionHandler);
    ReflectionTestUtils.setField(showTypeSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(showTypeSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(showTypeSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(showTypeSyncService, "showTypeService", showTypeService);
    ReflectionTestUtils.setField(showTypeSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  @DisplayName("Should extract show types from Notion and create them in database")
  void shouldExtractAndCreateShowTypesFromNotion() {
    // Given
    List<ShowPage> mockShowPages = createMockShowPages();
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.empty());
    when(showTypeService.findAll()).thenReturn(List.of());
    when(showTypeService.save(any(ShowType.class)))
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
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(notionHandler.loadAllShowsForSync()).thenReturn(Collections.emptyList());
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.empty());
    when(showTypeService.save(any(ShowType.class)))
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
    when(showTypeService.findAll()).thenReturn(initialShowTypes);
    when(notionHandler.loadAllShowsForSync()).thenReturn(createMockShowPages());
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(initialShowTypes.get(0)));
    when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.of(initialShowTypes.get(1)));
    when(showTypeService.save(any(ShowType.class)))
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
    when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(existingType));
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.empty());
    when(showTypeService.findAll())
        .thenReturn(List.of(existingType)); // Mock findAll to prevent default saves
    when(notionHandler.loadAllShowsForSync()).thenReturn(createMockShowPages());

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

    when(notionHandler.loadAllShowsForSync()).thenThrow(new RuntimeException("Notion API error"));

    when(showTypeService.findAll())
        .thenReturn(Collections.emptyList()); // Ensure default types are created

    when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.empty());

    when(showTypeService.save(any(ShowType.class)))
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
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(showTypeService.findByName(anyString())).thenReturn(Optional.empty());
    when(showTypeService.save(any(ShowType.class)))
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
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    when(showTypeService.findAll())
        .thenReturn(existingShowTypes); // Return existing types to prevent default saves
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(existingShowTypes.get(0)));
    when(showTypeService.findByName("Premium Live Event (PLE)"))
        .thenReturn(Optional.of(existingShowTypes.get(1)));
    when(showTypeService.save(any(ShowType.class)))
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

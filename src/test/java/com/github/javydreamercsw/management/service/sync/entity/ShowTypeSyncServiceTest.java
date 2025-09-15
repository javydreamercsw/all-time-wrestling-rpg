package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.ArrayList;
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
class ShowTypeSyncServiceTest extends BaseTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  private final NotionSyncProperties syncProperties; // Declare without @Mock
  @Mock private ShowTypeService showTypeService;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock public NotionRateLimitService rateLimitService;

  private ShowTypeSyncService showTypeSyncService;

  // Constructor to configure the mock before setUp()
  public ShowTypeSyncServiceTest() {
    syncProperties = mock(NotionSyncProperties.class); // Manually create mock
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
  }

  @BeforeEach
  void setUp() {
    showTypeSyncService = new ShowTypeSyncService(objectMapper, syncProperties);
    setField(showTypeSyncService, "notionHandler", notionHandler);
    setField(showTypeSyncService, "progressTracker", progressTracker);
    setField(showTypeSyncService, "healthMonitor", healthMonitor);
    setField(showTypeSyncService, "showTypeService", showTypeService);
    setField(showTypeSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  @DisplayName("Should extract show types from Notion and create them in database")
  void shouldExtractAndCreateShowTypesFromNotion() {
    List<ShowPage> mockShowPages = createMockShowPages();
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Premium Live Event (PLE)")).thenReturn(Optional.empty());
    when(showTypeService.findAll()).thenReturn(List.of());
    when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes("test-operation-id");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Show Types");
    assertThat(result.getSyncedCount()).isGreaterThan(0);
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
}

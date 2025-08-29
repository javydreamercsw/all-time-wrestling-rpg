package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.lang.reflect.Field;
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
class ShowTypeSyncServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private ShowTypeService showTypeService;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;

  private ShowTypeSyncService showTypeSyncService;

  @BeforeEach
  void setUp() {
    showTypeSyncService = new ShowTypeSyncService(objectMapper, syncProperties);
    setField(showTypeSyncService, "notionHandler", notionHandler);
    setField(showTypeSyncService, "progressTracker", progressTracker);
    setField(showTypeSyncService, "healthMonitor", healthMonitor);
    setField(showTypeSyncService, "showTypeService", showTypeService);
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

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      try {
        Field superField = target.getClass().getSuperclass().getDeclaredField(fieldName);
        superField.setAccessible(true);
        superField.set(target, value);
      } catch (Exception e2) {
        throw new RuntimeException("Failed to set field " + fieldName, e2);
      }
    }
  }
}

package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitleReignSyncServiceTest {

  @Mock private TitleReignRepository titleReignRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private ObjectMapper objectMapper; // Needed for constructor
  @Mock private NotionHandler notionHandler;
  @Mock private SyncHealthMonitor healthMonitor;

  @Mock
  private com.github.javydreamercsw.management.service.sync.NotionRateLimitService rateLimitService;

  private TitleReignSyncService service;

  @BeforeEach
  void setUp() throws Exception {
    when(syncProperties.getParallelThreads()).thenReturn(1);
    service = new TitleReignSyncService(objectMapper, syncProperties);

    // Manually inject mocks into the private fields of the base class
    Field handlerField = BaseSyncService.class.getDeclaredField("notionHandler");
    handlerField.setAccessible(true);
    handlerField.set(service, notionHandler);

    Field healthMonitorField = BaseSyncService.class.getDeclaredField("healthMonitor");
    healthMonitorField.setAccessible(true);
    healthMonitorField.set(service, healthMonitor);

    Field rateLimitServiceField = BaseSyncService.class.getDeclaredField("rateLimitService");
    rateLimitServiceField.setAccessible(true);
    rateLimitServiceField.set(service, rateLimitService);

    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "titleReignRepository", titleReignRepository);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "titleRepository", titleRepository);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "wrestlerRepository", wrestlerRepository);

    service.clearSyncSession();
  }

  @Test
  void syncTitleReigns_whenAlreadySynced_shouldSkip() {
    // Given: A sync has already run successfully in this session.
    when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);
    when(notionHandler.loadAllTitleReigns()).thenReturn(Collections.emptyList());
    SyncResult firstResult = service.syncTitleReigns("first-op"); // First call
    assertTrue(firstResult.isSuccess()); // Ensure the first sync completes

    // When: The sync is called a second time.
    SyncResult result = service.syncTitleReigns("second-op");

    // Then: The sync should be skipped.
    assertTrue(result.isSuccess());
    // Notion should only have been called once.
    verify(notionHandler, times(1)).loadAllTitleReigns();
  }

  @Test
  void syncTitleReigns_whenDisabled_shouldSkip() {
    // Given: The entity sync is disabled in properties.
    when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(false);

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should be skipped.
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllTitleReigns();
  }

  @Test
  void syncTitleReigns_whenSuccessful_shouldSaveReigns() {
    // Given: Notion returns a valid title reign page.
    when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);

    TitleReignPage page = new TitleReignPage();
    page.setId("page-id-1");
    Map<String, Object> props = new HashMap<>();
    props.put("Title", "title-id-1");
    props.put("Champion", "wrestler-id-1");
    props.put("Reign Number", 1);
    props.put("Start Date", "2023-01-15");
    page.setRawProperties(props);
    List<TitleReignPage> pages = Collections.singletonList(page);

    when(notionHandler.loadAllTitleReigns()).thenReturn(pages);

    Title title = new Title();
    title.setExternalId("title-id-1");
    title.setName("World Championship");
    when(titleRepository.findByExternalId("title-id-1")).thenReturn(Optional.of(title));

    Wrestler wrestler = new Wrestler();
    wrestler.setExternalId("wrestler-id-1");
    wrestler.setName("Champion Wrestler");
    when(wrestlerRepository.findByExternalId("wrestler-id-1")).thenReturn(Optional.of(wrestler));

    when(titleReignRepository.findByTitleAndReignNumber(title, 1)).thenReturn(Optional.empty());

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should succeed and a new reign should be saved.
    assertTrue(result.isSuccess());
    verify(titleReignRepository).save(any(TitleReign.class));
    // Verify that the saved reign has the correct champion(s)
    ArgumentCaptor<TitleReign> reignCaptor = ArgumentCaptor.forClass(TitleReign.class);
    verify(titleReignRepository).save(reignCaptor.capture());
    TitleReign savedReign = reignCaptor.getValue();
    assertThat(savedReign.getChampions()).containsExactly(wrestler);
    verify(healthMonitor).recordSuccess(anyString(), anyLong(), anyInt());
  }

  @Test
  void syncTitleReigns_whenTitleNotFound_shouldSkipReign() {
    // Given: Notion returns a page where the related title does not exist locally.
    when(syncProperties.isEntityEnabled("titlereigns")).thenReturn(true);

    TitleReignPage page = new TitleReignPage();
    page.setId("page-id-1");
    Map<String, Object> props = new HashMap<>();
    props.put("Title", "non-existent-title-id");
    props.put("Champion", "wrestler-id-1");
    page.setRawProperties(props);
    List<TitleReignPage> pages = Collections.singletonList(page);

    when(notionHandler.loadAllTitleReigns()).thenReturn(pages);

    when(titleRepository.findByExternalId("non-existent-title-id")).thenReturn(Optional.empty());

    // When: The sync is executed.
    SyncResult result = service.syncTitleReigns("test-op");

    // Then: The sync should still report success, but no reign should be saved.
    assertTrue(result.isSuccess());
    verify(titleReignRepository, never()).save(any(TitleReign.class));
  }
}

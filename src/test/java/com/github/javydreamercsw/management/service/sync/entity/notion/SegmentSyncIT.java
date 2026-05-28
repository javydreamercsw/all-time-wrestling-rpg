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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@DisplayName("Segment Sync Integration Tests")
class SegmentSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @MockitoBean private NotionHandler notionHandler;

  @MockitoBean
  private com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor notionPageDataExtractor;

  private SegmentPage segmentPage;

  private static MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

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
    if (mockedEnvironmentVariableUtil != null) {
      mockedEnvironmentVariableUtil.close();
    }
  }

  @BeforeEach
  public void setUp() {
    clearAllRepositories();
    segmentPage = Mockito.mock(SegmentPage.class);
  }

  @Test
  @DisplayName("Should sync segments from Notion to database successfully")
  void shouldSyncSegmentsFromNotionToDatabaseSuccessfully() {
    // Given
    Wrestler wrestler1 = createTestWrestler("Wrestler 1");
    wrestler1.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.saveAndFlush(wrestler1);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2");
    wrestler2.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.saveAndFlush(wrestler2);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setExternalId("test-show-id");
    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("A test show type");
    showTypeRepository.saveAndFlush(showType);
    show.setType(showType);
    showRepository.saveAndFlush(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Segment Type");
    segmentType.setExternalId("test-segtype-id");
    segmentTypeRepository.saveAndFlush(segmentType);

    String segmentId = UUID.randomUUID().toString();
    when(segmentPage.getId()).thenReturn(segmentId);

    SegmentPage.NotionProperties properties = mock(SegmentPage.NotionProperties.class);
    NotionPage.Property shows = mock(NotionPage.Property.class);
    when(properties.getShows()).thenReturn(shows);
    NotionPage.Relation relation = new NotionPage.Relation();
    relation.setId("test-show-id");
    when(shows.getRelation()).thenReturn(List.of(relation));

    when(segmentPage.getProperties()).thenReturn(properties);

    when(segmentPage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Test Segment",
                "Show",
                "test-show-id",
                "Participants",
                wrestler1.getName() + "," + wrestler2.getName(),
                "Winners",
                wrestler1.getName(),
                "Segment Type",
                segmentType.getExternalId(),
                "Date",
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))));

    when(notionHandler.getDatabasePageIds("Segments")).thenReturn(List.of(segmentId));
    when(notionHandler.loadSegmentById(segmentId)).thenReturn(Optional.of(segmentPage));
    // Mock convenience method used by service to load all segments
    when(notionHandler.loadAllSegments()).thenReturn(List.of(segmentPage));

    // Mock extractor behavior for relations and participants
    when(notionPageDataExtractor.extractRelationId(segmentPage, "Shows"))
        .thenReturn("test-show-id");
    when(notionPageDataExtractor.extractRelationId(segmentPage, "Segment Type"))
        .thenReturn(segmentType.getExternalId());
    when(notionPageDataExtractor.extractRelationIds(segmentPage, "Participants"))
        .thenReturn(List.of(wrestler1.getExternalId(), wrestler2.getExternalId()));
    when(notionPageDataExtractor.extractRelationIds(segmentPage, "Winners"))
        .thenReturn(List.of(wrestler1.getExternalId()));
    when(notionPageDataExtractor.extractNameFromNotionPage(segmentPage)).thenReturn("Test Segment");

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncSegments("test-operation", SyncDirection.INBOUND);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    transactionTemplate.executeWithoutResult(
        status -> {
          List<Segment> finalSegments = segmentRepository.findAll();
          assertThat(finalSegments).hasSize(1);
          Segment segment = finalSegments.get(0);
          assertThat(segment.getExternalId()).isEqualTo(segmentId);

          // Now safe to access lazy relationships
          assertThat(segment.getShow().getName()).isEqualTo("Test Show");
          assertThat(segment.getParticipants()).hasSize(2);
          assertThat(segment.getWinners()).hasSize(1);
          assertThat(segment.getWinners().get(0).getName()).isEqualTo(wrestler1.getName());
        });
  }

  @Test
  @DisplayName("Segment date with @ prefix and trailing time suffix is parsed to correct date")
  void shouldParseSegmentDateWithAtPrefixAndTimeSuffix() {
    // Regression: Notion sends dates like "@January 21, 2026 12:00 AM"; the @-prefix removal and
    // trailing time strip must both fire so the date parses to 2026-01-21T00:00:00Z.
    Show show = new Show();
    show.setName("Date Parse Show");
    show.setDescription("Test");
    show.setShowDate(LocalDate.of(2026, 1, 21));
    show.setExternalId("date-parse-show-id");
    ShowType showType = new ShowType();
    showType.setName("Date Parse Show Type");
    showType.setDescription("A test show type");
    showTypeRepository.saveAndFlush(showType);
    show.setType(showType);
    showRepository.saveAndFlush(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Date Parse Segment Type");
    segmentType.setExternalId("date-parse-segtype-id");
    segmentTypeRepository.saveAndFlush(segmentType);

    String segmentId = UUID.randomUUID().toString();
    SegmentPage datePage = Mockito.mock(SegmentPage.class);
    when(datePage.getId()).thenReturn(segmentId);

    SegmentPage.NotionProperties properties = mock(SegmentPage.NotionProperties.class);
    NotionPage.Property shows = mock(NotionPage.Property.class);
    when(properties.getShows()).thenReturn(shows);
    NotionPage.Relation relation = new NotionPage.Relation();
    relation.setId("date-parse-show-id");
    when(shows.getRelation()).thenReturn(List.of(relation));
    when(datePage.getProperties()).thenReturn(properties);

    when(datePage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name", "Date Parse Segment",
                "Date", "@January 21, 2026 12:00 AM",
                "Segment Type", segmentType.getExternalId()));

    lenient().when(notionHandler.getDatabasePageIds("Segments")).thenReturn(List.of(segmentId));
    lenient().when(notionHandler.loadSegmentById(segmentId)).thenReturn(Optional.of(datePage));
    when(notionHandler.loadAllSegments()).thenReturn(List.of(datePage));

    when(notionPageDataExtractor.extractRelationId(datePage, "Shows"))
        .thenReturn("date-parse-show-id");
    when(notionPageDataExtractor.extractRelationId(datePage, "Segment Type"))
        .thenReturn(segmentType.getExternalId());
    lenient()
        .when(notionPageDataExtractor.extractRelationIds(datePage, "Participants"))
        .thenReturn(List.of());
    lenient()
        .when(notionPageDataExtractor.extractRelationIds(datePage, "Winners"))
        .thenReturn(List.of());
    when(notionPageDataExtractor.extractNameFromNotionPage(datePage))
        .thenReturn("Date Parse Segment");

    BaseSyncService.SyncResult result =
        notionSyncService.syncSegments("date-parse-op", SyncDirection.INBOUND);

    assertThat(result.isSuccess()).isTrue();
    transactionTemplate.executeWithoutResult(
        status -> {
          List<Segment> segments = segmentRepository.findAll();
          assertThat(segments).hasSize(1);
          Instant expectedDate = Instant.parse("2026-01-21T00:00:00Z");
          assertThat(segments.get(0).getSegmentDate()).isEqualTo(expectedDate);
        });
  }
}

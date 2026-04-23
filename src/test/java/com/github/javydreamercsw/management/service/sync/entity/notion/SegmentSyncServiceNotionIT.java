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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Segment Sync Integration Tests")
class SegmentSyncServiceNotionIT extends ManagementIntegrationTest {

  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository
      segmentTypeRepository;

  @MockitoBean private NotionHandler notionHandler;

  @MockitoBean
  private com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor notionPageDataExtractor;

  private Show testShow;

  @BeforeEach
  public void setUp() {
    segmentRepository.deleteAll();

    // Create a ShowType required by Show (show_type_id is NOT NULL)
    com.github.javydreamercsw.management.domain.show.type.ShowType showType =
        new com.github.javydreamercsw.management.domain.show.type.ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test Show Type Description");
    showType = showTypeRepository.save(showType);

    // Create a Show that segments can be linked to (show_id is NOT NULL in the schema)
    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test Show Description");
    testShow.setExternalId("test-show-ext-id");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);
  }

  @Test
  @DisplayName("Should sync segments from Notion successfully")
  void shouldSyncSegmentsFromNotionSuccessfully() {
    // Given
    String segmentId = UUID.randomUUID().toString();
    SegmentPage segmentPage = Mockito.mock(SegmentPage.class);
    when(segmentPage.getId()).thenReturn(segmentId);
    when(segmentPage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name", "Test Segment",
                "Status", "BOOKED",
                "Adjudication Status", "PENDING"));
    when(notionHandler.loadAllSegments()).thenReturn(List.of(segmentPage));

    // Create and save required segment type and mock extractor
    com.github.javydreamercsw.management.domain.show.segment.type.SegmentType segmentType =
        new com.github.javydreamercsw.management.domain.show.segment.type.SegmentType();
    segmentType.setName("Default Segment Type");
    segmentType.setExternalId("test-segtype-id");
    segmentTypeRepository.save(segmentType);
    when(notionPageDataExtractor.extractRelationId(segmentPage, "Segment Type"))
        .thenReturn(segmentType.getExternalId());

    // Link the segment to the test show
    when(notionPageDataExtractor.extractRelationId(segmentPage, "Shows"))
        .thenReturn(testShow.getExternalId());

    // When
    BaseSyncService.SyncResult result = segmentSyncService.syncSegments("test-op-1");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<Segment> segments = segmentRepository.findAll();
    assertThat(segments).hasSize(1);
    assertThat(segments.get(0).getExternalId()).isEqualTo(segmentId);
  }
}

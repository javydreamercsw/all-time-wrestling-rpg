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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("Segment Sync Integration Tests")
class SegmentSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync segments from Notion to database successfully")
  @Transactional
  void shouldSyncSegmentsFromNotionToDatabaseSuccessfully() {
    // Given
    Wrestler wrestler1 = createTestWrestler("Wrestler 1");
    wrestlerRepository.saveAndFlush(wrestler1);
    Wrestler wrestler2 = createTestWrestler("Wrestler 2");
    wrestlerRepository.saveAndFlush(wrestler2);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setExternalId("test-show-id");
    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("A test show type");
    showTypeRepository.save(showType);
    show.setType(showType);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Segment Type");
    segmentTypeRepository.save(segmentType);

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncSegments("test-operation", SyncDirection.OUTBOUND);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<Segment> finalSegments = segmentRepository.findAll();
    assertThat(finalSegments).hasSize(1);
    Segment segment = finalSegments.get(0);
    assertThat(segment.getExternalId()).isNotNull();
    assertThat(segment.getShow().getName()).isEqualTo("Test Show");
    assertThat(segment.getParticipants()).hasSize(2);
    assertThat(segment.getWinners()).hasSize(1);
    assertThat(segment.getWinners().get(0).getName()).isEqualTo(wrestler1.getName());
  }
}

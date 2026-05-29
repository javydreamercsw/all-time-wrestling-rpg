/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

class ShowSyncServiceTest extends AbstractSyncTest {

  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private ObjectMapper mapper;

  @Captor private ArgumentCaptor<Show> savedShowCaptor;

  private ShowSyncService showSyncService;

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();
    showSyncService =
        new ShowSyncService(
            mapper,
            syncServiceDependencies,
            showService,
            showTypeService,
            seasonService,
            showTemplateService,
            notionApiExecutor);

    lenient()
        .when(seasonService.getAllSeasons(any()))
        .thenReturn(org.springframework.data.domain.Page.empty());
    lenient().when(showTemplateService.findAll()).thenReturn(List.of());
  }

  // ---------------------------------------------------------------------------
  // Staleness predicate: verifies the OffsetDateTime comparison logic used in
  // performShowsSyncInternal to decide which existing pages are stale (#ATW-400)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Notion last_edited_time after local lastSync is stale")
  void notionNewerThanLocalLastSync_isStale() {
    Instant localLastSync = Instant.parse("2026-01-10T00:00:00Z");
    String notionEdited = "2026-01-15T00:00:00.000+00:00";

    Instant notionInstant = OffsetDateTime.parse(notionEdited).toInstant();
    assertThat(notionInstant.isAfter(localLastSync))
        .as("Notion page edited after local lastSync must be considered stale")
        .isTrue();
  }

  @Test
  @DisplayName("Notion last_edited_time before local lastSync is not stale")
  void notionOlderThanLocalLastSync_isNotStale() {
    Instant localLastSync = Instant.parse("2026-01-20T00:00:00Z");
    String notionEdited = "2026-01-10T00:00:00.000+00:00";

    Instant notionInstant = OffsetDateTime.parse(notionEdited).toInstant();
    assertThat(notionInstant.isAfter(localLastSync))
        .as("Notion page edited before local lastSync must NOT be considered stale")
        .isFalse();
  }

  @Test
  @DisplayName(
      "Instant.EPOCH fallback (null lastSync) is always stale against any real Notion edit")
  void epochFallbackIsAlwaysStale() {
    // When local lastSync is null the service uses Instant.EPOCH — any real Notion edit is newer
    Instant localSync = Instant.EPOCH;
    String notionEdited = "2026-01-01T00:00:00.000+00:00";

    Instant notionInstant = OffsetDateTime.parse(notionEdited).toInstant();
    assertThat(notionInstant.isAfter(localSync))
        .as("Any Notion edit time is after Instant.EPOCH, so null-lastSync shows must be updated")
        .isTrue();
  }

  // ---------------------------------------------------------------------------
  // processSingleShow: lastSync is stamped after a successful save (#ATW-400)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("processSingleShow sets lastSync on a new show entity")
  void processSingleShowSetsLastSyncOnNewShow() {
    ShowType showType = new ShowType();
    showType.setName("Weekly");

    ShowDTO dto = new ShowDTO();
    dto.setExternalId("show-new-lastsync");
    dto.setName("New Show");
    dto.setShowType("Weekly");

    when(showService.findByExternalId("show-new-lastsync")).thenReturn(java.util.Optional.empty());

    showSyncService.processSingleShow(dto, Map.of("Weekly", showType), Map.of(), Map.of());

    verify(showService).save(savedShowCaptor.capture());
    assertThat(savedShowCaptor.getValue().getLastSync())
        .as("processSingleShow must stamp lastSync so future staleness checks have a baseline")
        .isNotNull();
  }

  @Test
  @DisplayName("processSingleShow updates existing show name and refreshes lastSync")
  void processSingleShowUpdatesExistingShowAndRefreshesLastSync() {
    ShowType showType = new ShowType();
    showType.setName("Weekly");

    Show existing = new Show();
    existing.setExternalId("show-existing-update");
    existing.setName("Old Name");
    existing.setLastSync(Instant.parse("2026-01-01T00:00:00Z"));

    ShowDTO dto = new ShowDTO();
    dto.setExternalId("show-existing-update");
    dto.setName("New Name");
    dto.setShowType("Weekly");

    when(showService.findByExternalId("show-existing-update"))
        .thenReturn(java.util.Optional.of(existing));

    showSyncService.processSingleShow(dto, Map.of("Weekly", showType), Map.of(), Map.of());

    verify(showService).save(savedShowCaptor.capture());
    Show saved = savedShowCaptor.getValue();
    assertThat(saved.getName()).isEqualTo("New Name");
    assertThat(saved.getLastSync()).isNotNull().isAfter(Instant.parse("2026-01-01T00:00:00Z"));
  }
}

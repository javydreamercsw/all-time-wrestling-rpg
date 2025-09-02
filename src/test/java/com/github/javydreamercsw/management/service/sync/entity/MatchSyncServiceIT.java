package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.service.match.MatchResultService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"notion.sync.scheduler.enabled=false", "notion.databases.show-types=test-db-id"})
class MatchSyncServiceIT {

  @Autowired private MatchSyncService matchSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowSyncService showSyncService;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @Autowired private MatchResultService matchResultService;

  @BeforeEach
  void setup() {
    seasonSyncService.syncSeasons(UUID.randomUUID().toString());
    wrestlerSyncService.syncWrestlers(UUID.randomUUID().toString());
    showTemplateSyncService.syncShowTemplates(UUID.randomUUID().toString());
    showTypeSyncService.syncShowTypes(UUID.randomUUID().toString());
    showSyncService.syncShows(UUID.randomUUID().toString());
  }

  @Test
  void testSyncMatches() {
    // Make sure we have data to test against.
    // Assuming the Notion database has at least one match.
    matchSyncService.syncMatches(UUID.randomUUID().toString());
    assertThat(
            matchResultService
                .getAllMatchResults(org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements())
        .isPositive();
  }
}

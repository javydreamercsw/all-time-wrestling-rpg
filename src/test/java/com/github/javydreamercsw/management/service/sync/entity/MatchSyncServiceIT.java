package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"notion.sync.scheduler.enabled=false", "notion.databases.show-types=test-db-id"})
@EnabledIf("isNotionTokenAvailable")
class MatchSyncServiceIT extends BaseTest {

  @Autowired private MatchSyncService matchSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowSyncService showSyncService;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @Autowired private MatchResultService matchResultService;

  @Test
  void testSyncMatch() {
    // Make sure we have data to test against.
    // Assuming the Notion database has at least one match.
    matchSyncService.syncMatch(matchSyncService.getMatchIds().get(0));
    assertThat(
            matchResultService
                .getAllMatchResults(org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements())
        .isPositive();
  }
}

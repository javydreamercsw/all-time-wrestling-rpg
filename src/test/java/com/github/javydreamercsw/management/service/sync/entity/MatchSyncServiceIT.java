package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.service.match.MatchService;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("MatchSyncService Integration Tests")
class MatchSyncServiceIT extends BaseTest {

  @Autowired private MatchSyncService matchSyncService;
  @Autowired private MatchService matchService;

  @Test
  @DisplayName("Should sync a single match by ID")
  void shouldSyncSingleMatchById() {
    List<String> matchIds = matchSyncService.getMatchIds();
    Random r = new Random();
    Arrays.asList(
            matchIds.get(r.nextInt(matchIds.size())), matchIds.get(r.nextInt(matchIds.size())))
        .forEach(
            knownMatchId -> { // When
              MatchSyncService.SyncResult result = matchSyncService.syncMatch(knownMatchId);
              // Then
              assertThat(result).isNotNull();
              assertThat(result.isSuccess()).isTrue();
              assertThat(result.getSyncedCount()).isEqualTo(1);
              assertThat(matchService.findByExternalId(knownMatchId)).isPresent();
            });
  }

  @Test
  @DisplayName("Should return failure for non-existent match ID")
  void shouldReturnFailureForNonExistentMatchId() {
    MatchSyncService.SyncResult result = matchSyncService.syncMatch(UUID.randomUUID().toString());

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("not found in Notion");
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }
}

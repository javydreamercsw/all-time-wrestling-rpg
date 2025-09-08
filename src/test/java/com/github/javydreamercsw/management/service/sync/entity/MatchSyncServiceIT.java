package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.service.match.MatchService;
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
class MatchSyncServiceIT {

  @Autowired private MatchSyncService matchSyncService;
  @Autowired private MatchService matchService;

  @Test
  @DisplayName("Should sync a single match by ID")
  void shouldSyncSingleMatchById() {
    // Given - A known match ID from Notion (replace with a real one from your Notion DB)
    // For integration tests, you'd typically have a setup that ensures this ID exists
    String knownMatchId = "YOUR_NOTION_MATCH_ID_HERE"; // Replace with a valid Notion Match ID

    // When
    MatchSyncService.SyncResult result = matchSyncService.syncMatch(knownMatchId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);
    assertThat(matchService.findByExternalId(knownMatchId)).isPresent();
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

  @Test
  @DisplayName("Should get all match IDs")
  void shouldGetAllMatchIds() {
    // When
    java.util.List<String> matchIds = matchSyncService.getMatchIds();

    // Then
    assertThat(matchIds).isNotNull();
    // Assert that the list is not empty if you expect matches to exist in Notion
    // assertThat(matchIds).isNotEmpty();
  }
}

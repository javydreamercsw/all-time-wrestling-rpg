package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.service.match.MatchService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchSyncService Tests")
class MatchSyncServiceTest extends BaseTest {

  @Mock private NotionHandler notionHandler;
  @Mock private MatchService matchService;
  @Mock private ShowService showService;
  @Mock private MatchTypeService matchTypeService;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private ObjectMapper objectMapper;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private NotionRateLimitService rateLimitService;

  private MatchSyncService matchSyncService;

  @BeforeEach
  void setUp() {
    matchSyncService = new MatchSyncService(objectMapper, syncProperties);

    // Manually inject the mocked dependencies using reflection
    setField(matchSyncService, "notionHandler", notionHandler);
    setField(matchSyncService, "progressTracker", progressTracker);
    setField(matchSyncService, "rateLimitService", rateLimitService);
    setField(matchSyncService, "matchService", matchService);
    setField(matchSyncService, "showService", showService);
    setField(matchSyncService, "matchTypeService", matchTypeService);
  }

  @Test
  @DisplayName("Should sync new matches successfully")
  void shouldSyncNewMatchesSuccessfully() {
    // Given
    MatchPage matchPage = new MatchPage();
    matchPage.setId("ext1");
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());
    matchPage.setProperties(new MatchPage.NotionProperties());
    matchPage
        .getProperties()
        .setShows(
            new MatchPage.Property() {
              {
                setType("relation");
                setRelation(
                    List.of(
                        new NotionPage.Relation() {
                          {
                            setId("dummyShowExternalId");
                          }
                        }));
              }
            });
    matchPage.setRawProperties(
        Map.of("Name", "Test Match", "Match Type", "Singles", "Participants", "", "Winners", ""));

    when(notionHandler.loadMatchById(anyString())).thenReturn(Optional.of(matchPage));
    when(matchService.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(showService.findByExternalId(anyString())).thenReturn(Optional.of(new Show()));
    when(matchTypeService.findByName(anyString())).thenReturn(Optional.of(new MatchType()));

    // When
    SyncResult result = matchSyncService.syncMatch("ext1");

    // Then
    verify(matchService, times(1)).updateMatch(any(Match.class));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should update existing matches successfully")
  void shouldUpdateExistingMatchesSuccessfully() {
    // Given
    MatchPage matchPage = new MatchPage();
    matchPage.setId("ext1");
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());
    matchPage.setProperties(new MatchPage.NotionProperties());
    matchPage
        .getProperties()
        .setShows(
            new MatchPage.Property() {
              {
                setType("relation");
                setRelation(
                    List.of(
                        new NotionPage.Relation() {
                          {
                            setId("dummyShowExternalId");
                          }
                        }));
              }
            });
    matchPage.setRawProperties(
        Map.of(
            "Name",
            "Updated Test Match",
            "Match Type",
            "Singles",
            "Participants",
            "",
            "Winners",
            ""));

    Match existingMatch = new Match();
    existingMatch.setId(1L);
    existingMatch.setExternalId("ext1");

    when(notionHandler.loadMatchById(anyString())).thenReturn(Optional.of(matchPage));
    when(matchService.findByExternalId(anyString())).thenReturn(Optional.of(existingMatch));
    when(showService.findByExternalId(anyString())).thenReturn(Optional.of(new Show()));
    when(matchTypeService.findByName(anyString())).thenReturn(Optional.of(new MatchType()));

    // When
    SyncResult result = matchSyncService.syncMatch("ext1");

    // Then
    verify(matchService, times(1)).updateMatch(any(Match.class));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);
  }

  @DisplayName("Should handle no match found in Notion")
  void shouldHandleNoMatchFoundInNotion() {
    // Given
    when(notionHandler.loadMatchById(anyString())).thenReturn(Optional.empty());

    // When
    SyncResult result = matchSyncService.syncMatch("non-existent-id");

    // Then
    verify(matchService, times(0)).updateMatch(any(Match.class));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle exception during Notion fetch")
  void shouldHandleExceptionDuringNotionFetch() {
    // Given
    when(notionHandler.loadMatchById(anyString()))
        .thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = matchSyncService.syncMatch("any-id");

    // Then
    Assertions.assertFalse(result.isSuccess());
    verify(matchService, times(0)).updateMatch(any(Match.class));
  }
}
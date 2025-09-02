package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncValidationService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchSyncService Tests")
class MatchSyncServiceTest {

  @Mock private NotionHandler notionHandler;
  @Mock private MatchResultService matchResultService;
  @Mock private ShowService showService;
  @Mock private WrestlerService wrestlerService;
  @Mock private MatchTypeService matchTypeService;
  @Mock private SyncProgressTracker syncProgressTracker;
  @Mock private SyncValidationService syncValidationService;

  private MatchSyncService matchSyncService;

  @BeforeEach
  void setUp() {
    matchSyncService =
        new MatchSyncService(
            notionHandler,
            matchResultService,
            showService,
            wrestlerService,
            matchTypeService,
            syncProgressTracker,
            syncValidationService);
  }

  @Test
  @DisplayName("Should sync new matches successfully")
  void shouldSyncNewMatchesSuccessfully() {
    // Given
    MatchPage matchPage = new MatchPage();
    matchPage.setId("ext1");
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());
    matchPage.setRawProperties(
        Map.of(
            "Name",
            "Test Match",
            "Shows",
            new NotionPage.Property() {
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
            },
            "Match Type",
            "Singles",
            "Participants",
            "",
            "Winners",
            ""));

    when(notionHandler.loadAllMatches()).thenReturn(List.of(matchPage));
    when(matchResultService.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(showService.findByExternalId(anyString())).thenReturn(Optional.of(new Show()));
    when(matchTypeService.findByName(anyString())).thenReturn(Optional.of(new MatchType()));

    // When
    SyncResult result = matchSyncService.syncMatches(UUID.randomUUID().toString());

    // Then
    verify(matchResultService, times(1)).updateMatchResult(any(MatchResult.class));
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
    matchPage.setRawProperties(
        Map.of(
            "Name",
            "Updated Test Match",
            "Shows",
            new NotionPage.Property() {
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
            },
            "Match Type",
            "Singles",
            "Participants",
            "",
            "Winners",
            ""));

    MatchResult existingMatch = new MatchResult();
    existingMatch.setId(1L);
    existingMatch.setExternalId("ext1");

    when(notionHandler.loadAllMatches()).thenReturn(List.of(matchPage));
    when(matchResultService.findByExternalId(anyString())).thenReturn(Optional.of(existingMatch));
    when(showService.findByExternalId(anyString())).thenReturn(Optional.of(new Show()));
    when(matchTypeService.findByName(anyString())).thenReturn(Optional.of(new MatchType()));

    // When
    SyncResult result = matchSyncService.syncMatches(UUID.randomUUID().toString());

    // Then
    verify(matchResultService, times(1)).updateMatchResult(any(MatchResult.class));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle no matches found in Notion")
  void shouldHandleNoMatchesFoundInNotion() {
    // Given
    when(notionHandler.loadAllMatches()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = matchSyncService.syncMatches(UUID.randomUUID().toString());

    // Then
    verify(matchResultService, times(0)).updateMatchResult(any(MatchResult.class));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle exception during Notion fetch")
  void shouldHandleExceptionDuringNotionFetch() {
    // Given
    when(notionHandler.loadAllMatches()).thenThrow(new RuntimeException("Notion API error"));

    // When
    assertThrows(
        RuntimeException.class,
        () -> matchSyncService.syncMatches(UUID.randomUUID().toString()),
        "Notion API error");

    // Then
    verify(matchResultService, times(0)).updateMatchResult(any(MatchResult.class));
  }
}

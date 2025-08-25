package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Matches Sync Integration Tests")
class MatchesSyncIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;

  @MockBean private NotionHandler notionHandler;

  @Autowired private MatchResultRepository matchResultRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private MatchTypeRepository matchTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  private Show testShow;
  private MatchType testMatchType;
  private Wrestler testWrestler1;
  private Wrestler testWrestler2;
  private MatchPage testMatchPage;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    matchResultRepository.deleteAll();
    showRepository.deleteAll();
    wrestlerRepository.deleteAll();
    matchTypeRepository.deleteAll();
    showTypeRepository.deleteAll();

    // Create test data
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("Regular weekly show");
    showType = showTypeRepository.save(showType);

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for integration testing");
    testShow.setType(showType);
    testShow = showRepository.save(testShow);

    testMatchType = new MatchType();
    testMatchType.setName("Singles");
    testMatchType.setDescription("One-on-one match");
    testMatchType = matchTypeRepository.save(testMatchType);

    testWrestler1 = new Wrestler();
    testWrestler1.setName("Test Wrestler 1");
    testWrestler1.setDescription("First test wrestler");
    testWrestler1.setIsPlayer(false);
    testWrestler1.setTier(WrestlerTier.ROOKIE);
    testWrestler1.setStartingHealth(100);
    testWrestler1.setStartingStamina(100);
    testWrestler1.setLowHealth(20);
    testWrestler1.setLowStamina(20);
    testWrestler1.setDeckSize(40);
    testWrestler1.setFans(1000L);
    testWrestler1 = wrestlerRepository.save(testWrestler1);

    testWrestler2 = new Wrestler();
    testWrestler2.setName("Test Wrestler 2");
    testWrestler2.setDescription("Second test wrestler");
    testWrestler2.setIsPlayer(false);
    testWrestler2.setTier(WrestlerTier.ROOKIE);
    testWrestler2.setStartingHealth(100);
    testWrestler2.setStartingStamina(100);
    testWrestler2.setLowHealth(20);
    testWrestler2.setLowStamina(20);
    testWrestler2.setDeckSize(40);
    testWrestler2.setFans(1000L);
    testWrestler2 = wrestlerRepository.save(testWrestler2);

    // Create test MatchPage
    testMatchPage = createTestMatchPage();
  }

  private MatchPage createTestMatchPage() {
    MatchPage matchPage = new MatchPage();
    matchPage.setId("notion-match-123");
    matchPage.setCreated_time(Instant.now().toString());
    matchPage.setLast_edited_time(Instant.now().toString());

    // Create mock user objects
    NotionPage.NotionUser createdBy = new NotionPage.NotionUser();
    createdBy.setName("Test User");
    matchPage.setCreated_by(createdBy);

    NotionPage.NotionUser lastEditedBy = new NotionPage.NotionUser();
    lastEditedBy.setName("Test User");
    matchPage.setLast_edited_by(lastEditedBy);

    // Create raw properties map
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", "Test Match vs Test Match");
    rawProperties.put("Description", "Integration test match");
    rawProperties.put("Participants", Arrays.asList("Test Wrestler 1", "Test Wrestler 2"));
    rawProperties.put("Winner", "Test Wrestler 1");
    rawProperties.put("MatchType", "Singles");
    rawProperties.put("Show", "Test Show");
    rawProperties.put("Duration", 15);
    rawProperties.put("Rating", 4);
    rawProperties.put("Stipulation", "Standard Rules");

    matchPage.setRawProperties(rawProperties);

    return matchPage;
  }

  @Test
  @DisplayName("Should sync matches from Notion to database successfully")
  void shouldSyncMatchesFromNotionToDatabaseSuccessfully() {
    // Given
    List<MatchPage> matchPages = Arrays.asList(testMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-123");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify match was saved to database
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).hasSize(1);

    MatchResult savedMatch = savedMatches.get(0);
    assertThat(savedMatch.getExternalId()).isEqualTo("notion-match-123");
    assertThat(savedMatch.getShow()).isEqualTo(testShow);
    assertThat(savedMatch.getMatchType()).isEqualTo(testMatchType);
    assertThat(savedMatch.getWinner()).isEqualTo(testWrestler1);
    assertThat(savedMatch.getDurationMinutes()).isEqualTo(15);
    assertThat(savedMatch.getMatchRating()).isEqualTo(4);
  }

  @Test
  @DisplayName("Should skip duplicate matches during sync")
  void shouldSkipDuplicateMatchesDuringSync() {
    // Given - Create existing match with same external ID
    MatchResult existingMatch = new MatchResult();
    existingMatch.setExternalId("notion-match-123");
    existingMatch.setShow(testShow);
    existingMatch.setMatchType(testMatchType);
    existingMatch.setWinner(testWrestler1);
    existingMatch.setMatchDate(Instant.now());
    existingMatch.setIsTitleMatch(false);
    existingMatch.setIsNpcGenerated(false);
    matchResultRepository.save(existingMatch);

    List<MatchPage> matchPages = Arrays.asList(testMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-456");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0); // Should skip duplicate

    // Verify only one match exists in database
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).hasSize(1);
  }

  @Test
  @DisplayName("Should handle missing show gracefully")
  void shouldHandleMissingShowGracefully() {
    // Given - Match references non-existent show
    Map<String, Object> rawProperties = new HashMap<>(testMatchPage.getRawProperties());
    rawProperties.put("Show", "Non-existent Show");
    testMatchPage.setRawProperties(rawProperties);

    List<MatchPage> matchPages = Arrays.asList(testMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-789");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0); // Should skip invalid match

    // Verify no matches were saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).isEmpty();
  }

  @Test
  @DisplayName("Should handle missing match type gracefully")
  void shouldHandleMissingMatchTypeGracefully() {
    // Given - Match references non-existent match type
    Map<String, Object> rawProperties = new HashMap<>(testMatchPage.getRawProperties());
    rawProperties.put("MatchType", "Non-existent Type");
    testMatchPage.setRawProperties(rawProperties);

    List<MatchPage> matchPages = Arrays.asList(testMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-101");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0); // Should skip invalid match

    // Verify no matches were saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).isEmpty();
  }

  @Test
  @DisplayName("Should handle missing winner gracefully (draw scenario)")
  void shouldHandleMissingWinnerGracefully() {
    // Given - Match with no winner (draw)
    Map<String, Object> rawProperties = new HashMap<>(testMatchPage.getRawProperties());
    rawProperties.put("Winner", null);
    testMatchPage.setRawProperties(rawProperties);

    List<MatchPage> matchPages = Arrays.asList(testMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-202");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify match was saved with null winner
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).hasSize(1);
    assertThat(savedMatches.get(0).getWinner()).isNull();
  }

  @Test
  @DisplayName("Should handle empty match list from Notion")
  void shouldHandleEmptyMatchListFromNotion() {
    // Given
    when(notionHandler.loadAllMatches()).thenReturn(Arrays.asList());

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-303");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0);

    // Verify no matches were saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).isEmpty();
  }

  @Test
  @DisplayName("Should handle Notion handler exception gracefully")
  void shouldHandleNotionHandlerExceptionGracefully() {
    // Given
    when(notionHandler.loadAllMatches()).thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-404");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Notion API error");

    // Verify no matches were saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).isEmpty();
  }

  @Test
  @DisplayName("Should sync multiple matches correctly")
  void shouldSyncMultipleMatchesCorrectly() {
    // Given - Create second match page
    MatchPage secondMatchPage = createTestMatchPage();
    secondMatchPage.setId("notion-match-456");
    Map<String, Object> rawProperties = new HashMap<>(secondMatchPage.getRawProperties());
    rawProperties.put("Name", "Second Test Match");
    rawProperties.put("Winner", "Test Wrestler 2");
    secondMatchPage.setRawProperties(rawProperties);

    List<MatchPage> matchPages = Arrays.asList(testMatchPage, secondMatchPage);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-505");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(2);

    // Verify both matches were saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).hasSize(2);

    // Verify different winners
    List<Wrestler> winners = savedMatches.stream().map(MatchResult::getWinner).toList();
    assertThat(winners).containsExactlyInAnyOrder(testWrestler1, testWrestler2);
  }

  @Test
  @DisplayName("Should validate sync results correctly")
  void shouldValidateSyncResultsCorrectly() {
    // Given - Multiple matches with some invalid ones
    MatchPage validMatch = testMatchPage;

    MatchPage invalidMatch = createTestMatchPage();
    invalidMatch.setId("notion-match-invalid");
    Map<String, Object> invalidProperties = new HashMap<>(invalidMatch.getRawProperties());
    invalidProperties.put("Show", "Non-existent Show"); // Invalid show
    invalidMatch.setRawProperties(invalidProperties);

    List<MatchPage> matchPages = Arrays.asList(validMatch, invalidMatch);
    when(notionHandler.loadAllMatches()).thenReturn(matchPages);

    // When
    SyncResult result = notionSyncService.syncMatches("test-operation-606");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue(); // Should still succeed with partial sync
    assertThat(result.getSyncedCount()).isEqualTo(1); // Only valid match synced

    // Verify only valid match was saved
    List<MatchResult> savedMatches = matchResultRepository.findAll();
    assertThat(savedMatches).hasSize(1);
    assertThat(savedMatches.get(0).getExternalId()).isEqualTo("notion-match-123");
  }
}

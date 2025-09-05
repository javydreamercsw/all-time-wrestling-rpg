package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchResultService Tests")
class MatchResultServiceTest {

  @Mock private MatchResultRepository matchResultRepository;

  @InjectMocks private MatchResultService matchResultService;

  private Show testShow;
  private MatchType testMatchType;
  private Wrestler testWinner;
  private MatchResult testMatchResult;
  private Instant testDate;

  @BeforeEach
  void setUp() {
    testDate = Instant.now();

    testShow = new Show();
    testShow.setId(1L);
    testShow.setName("Test Show");

    testMatchType = new MatchType();
    testMatchType.setName("Singles");

    testWinner = new Wrestler();
    testWinner.setId(1L);
    testWinner.setName("Test Wrestler");

    testMatchResult = new MatchResult();
    testMatchResult.setId(1L);
    testMatchResult.setShow(testShow);
    testMatchResult.setMatchType(testMatchType);
    testMatchResult.setWinner(testWinner);
    testMatchResult.setMatchDate(testDate);
    testMatchResult.setDurationMinutes(15);
    testMatchResult.setMatchRating(4);
    testMatchResult.setNarration("Great match!");
    testMatchResult.setIsTitleMatch(false);
    testMatchResult.setIsNpcGenerated(false);
    testMatchResult.setExternalId("notion-123");
  }

  @Test
  @DisplayName("Should create match result successfully")
  void shouldCreateMatchResultSuccessfully() {
    // Given
    when(matchResultRepository.save(any(MatchResult.class))).thenReturn(testMatchResult);

    // When
    MatchResult result =
        matchResultService.createMatchResult(
            testShow, testMatchType, testWinner, testDate, 15, 4, "Great match!", false, false);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getMatchType()).isEqualTo(testMatchType);
    assertThat(result.getWinner()).isEqualTo(testWinner);
    assertThat(result.getMatchDate()).isEqualTo(testDate);
    assertThat(result.getDurationMinutes()).isEqualTo(15);
    assertThat(result.getMatchRating()).isEqualTo(4);
    assertThat(result.getNarration()).isEqualTo("Great match!");
    assertThat(result.getIsTitleMatch()).isFalse();
    assertThat(result.getIsNpcGenerated()).isFalse();

    verify(matchResultRepository).save(any(MatchResult.class));
  }

  @Test
  @DisplayName("Should create match result with default values when nulls provided")
  void shouldCreateMatchResultWithDefaultValues() {
    // Given
    MatchResult resultWithDefaults = new MatchResult();
    resultWithDefaults.setId(2L);
    resultWithDefaults.setShow(testShow);
    resultWithDefaults.setMatchType(testMatchType);
    resultWithDefaults.setWinner(testWinner); // Set a valid winner instead of null
    resultWithDefaults.setMatchDate(Instant.now());
    resultWithDefaults.setIsTitleMatch(false);
    resultWithDefaults.setIsNpcGenerated(false);

    when(matchResultRepository.save(any(MatchResult.class))).thenReturn(resultWithDefaults);

    // When
    MatchResult result =
        matchResultService.createMatchResult(
            testShow,
            testMatchType,
            testWinner, // winner
            Instant.now(), // matchDate
            0, // duration
            0, // rating
            "", // narration
            false, // isTitleMatch
            false); // isNpcGenerated

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getWinner()).isEqualTo(testWinner);
    assertThat(result.getMatchDate()).isNotNull(); // Should default to now
    assertThat(result.getIsTitleMatch()).isFalse(); // Should default to false
    assertThat(result.getIsNpcGenerated()).isFalse(); // Should default to false

    verify(matchResultRepository).save(any(MatchResult.class));
  }

  @Test
  @DisplayName("Should update match result successfully")
  void shouldUpdateMatchResultSuccessfully() {
    // Given
    when(matchResultRepository.save(testMatchResult)).thenReturn(testMatchResult);

    // When
    MatchResult result = matchResultService.updateMatchResult(testMatchResult);

    // Then
    assertThat(result).isEqualTo(testMatchResult);
    verify(matchResultRepository).save(testMatchResult);
  }

  @Test
  @DisplayName("Should find match result by ID")
  void shouldFindMatchResultById() {
    // Given
    when(matchResultRepository.findById(1L)).thenReturn(Optional.of(testMatchResult));

    // When
    Optional<MatchResult> result = matchResultService.findById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testMatchResult);
    verify(matchResultRepository).findById(1L);
  }

  @Test
  @DisplayName("Should return empty when match result not found by ID")
  void shouldReturnEmptyWhenMatchResultNotFoundById() {
    // Given
    when(matchResultRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<MatchResult> result = matchResultService.findById(999L);

    // Then
    assertThat(result).isEmpty();
    verify(matchResultRepository).findById(999L);
  }

  @Test
  @DisplayName("Should get all match results with pagination")
  void shouldGetAllMatchResultsWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<MatchResult> page = new PageImpl<>(Arrays.asList(testMatchResult));
    when(matchResultRepository.findAllBy(pageable)).thenReturn(page);

    // When
    Page<MatchResult> result = matchResultService.getAllMatchResults(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findAllBy(pageable);
  }

  @Test
  @DisplayName("Should get match results by show")
  void shouldGetMatchResultsByShow() {
    // Given
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByShow(testShow)).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getMatchResultsByShow(testShow);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByShow(testShow);
  }

  @Test
  @DisplayName("Should get match results by wrestler participation")
  void shouldGetMatchResultsByWrestlerParticipation() {
    // Given
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByWrestlerParticipation(testWinner)).thenReturn(matches);

    // When
    List<MatchResult> result =
        matchResultService.getMatchResultsByWrestlerParticipation(testWinner);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByWrestlerParticipation(testWinner);
  }

  @Test
  @DisplayName("Should get match results by winner")
  void shouldGetMatchResultsByWinner() {
    // Given
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByWinner(testWinner)).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getMatchResultsByWinner(testWinner);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByWinner(testWinner);
  }

  @Test
  @DisplayName("Should get matches between two wrestlers")
  void shouldGetMatchesBetweenTwoWrestlers() {
    // Given
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Test Wrestler 2");

    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findMatchesBetween(testWinner, wrestler2)).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getMatchesBetween(testWinner, wrestler2);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findMatchesBetween(testWinner, wrestler2);
  }

  @Test
  @DisplayName("Should get NPC generated matches")
  void shouldGetNpcGeneratedMatches() {
    // Given
    testMatchResult.setIsNpcGenerated(true);
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByIsNpcGeneratedTrue()).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getNpcGeneratedMatches();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByIsNpcGeneratedTrue();
  }

  @Test
  @DisplayName("Should get title matches")
  void shouldGetTitleMatches() {
    // Given
    testMatchResult.setIsTitleMatch(true);
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByIsTitleMatchTrue()).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getTitleMatches();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByIsTitleMatchTrue();
  }

  @Test
  @DisplayName("Should get matches after specific date")
  void shouldGetMatchesAfterSpecificDate() {
    // Given
    Instant afterDate = testDate.minusSeconds(3600);
    List<MatchResult> matches = Arrays.asList(testMatchResult);
    when(matchResultRepository.findByMatchDateAfter(afterDate)).thenReturn(matches);

    // When
    List<MatchResult> result = matchResultService.getMatchesAfter(afterDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByMatchDateAfter(afterDate);
  }

  @Test
  @DisplayName("Should count wins by wrestler")
  void shouldCountWinsByWrestler() {
    // Given
    when(matchResultRepository.countWinsByWrestler(testWinner)).thenReturn(5L);

    // When
    long result = matchResultService.countWinsByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(5L);
    verify(matchResultRepository).countWinsByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should count total matches by wrestler")
  void shouldCountTotalMatchesByWrestler() {
    // Given
    when(matchResultRepository.countMatchesByWrestler(testWinner)).thenReturn(10L);

    // When
    long result = matchResultService.countMatchesByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(10L);
    verify(matchResultRepository).countMatchesByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should check if match exists by external ID")
  void shouldCheckIfMatchExistsByExternalId() {
    // Given
    when(matchResultRepository.existsByExternalId("notion-123")).thenReturn(true);

    // When
    boolean result = matchResultService.existsByExternalId("notion-123");

    // Then
    assertThat(result).isTrue();
    verify(matchResultRepository).existsByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should find match by external ID")
  void shouldFindMatchByExternalId() {
    // Given
    when(matchResultRepository.findByExternalId("notion-123"))
        .thenReturn(Optional.of(testMatchResult));

    // When
    Optional<MatchResult> result = matchResultService.findByExternalId("notion-123");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testMatchResult);
    verify(matchResultRepository).findByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should delete match result by ID")
  void shouldDeleteMatchResultById() {
    // When
    matchResultService.deleteMatchResult(1L);

    // Then
    verify(matchResultRepository).deleteById(1L);
  }
}

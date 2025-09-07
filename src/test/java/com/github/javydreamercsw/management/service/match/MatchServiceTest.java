package com.github.javydreamercsw.management.service.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
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
@DisplayName("MatchService Tests")
class MatchServiceTest {

  @Mock private MatchRepository matchRepository;

  @InjectMocks private MatchService matchService;

  private Show testShow;
  private MatchType testMatchType;
  private Wrestler testWinner;
  private Match testMatch;
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

    testMatch = new Match();
    testMatch.setId(1L);
    testMatch.setShow(testShow);
    testMatch.setMatchType(testMatchType);
    testMatch.setWinner(testWinner);
    testMatch.setMatchDate(testDate);
    testMatch.setDurationMinutes(15);
    testMatch.setMatchRating(4);
    testMatch.setNarration("Great match!");
    testMatch.setIsTitleMatch(false);
    testMatch.setIsNpcGenerated(false);
    testMatch.setExternalId("notion-123");
  }

  @Test
  @DisplayName("Should create match successfully")
  void shouldCreateMatchSuccessfully() {
    // Given
    when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

    // When
    Match result =
        matchService.createMatch(
            testShow, testMatchType, testDate, false);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getShow()).isEqualTo(testShow);
    assertThat(result.getMatchType()).isEqualTo(testMatchType);
    assertThat(result.getMatchDate()).isEqualTo(testDate);
    assertThat(result.getIsTitleMatch()).isFalse();

    verify(matchRepository).save(any(Match.class));
  }

  @Test
  @DisplayName("Should update match successfully")
  void shouldUpdateMatchSuccessfully() {
    // Given
    when(matchRepository.save(testMatch)).thenReturn(testMatch);

    // When
    Match result = matchService.updateMatch(testMatch);

    // Then
    assertThat(result).isEqualTo(testMatch);
    verify(matchRepository).save(testMatch);
  }

  @Test
  @DisplayName("Should find match by ID")
  void shouldFindMatchById() {
    // Given
    when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

    // When
    Optional<Match> result = matchService.findById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testMatch);
    verify(matchRepository).findById(1L);
  }

  @Test
  @DisplayName("Should return empty when match not found by ID")
  void shouldReturnEmptyWhenMatchNotFoundById() {
    // Given
    when(matchRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<Match> result = matchService.findById(999L);

    // Then
    assertThat(result).isEmpty();
    verify(matchRepository).findById(999L);
  }

  @Test
  @DisplayName("Should get all matches with pagination")
  void shouldGetAllMatchesWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Match> page = new PageImpl<>(Arrays.asList(testMatch));
    when(matchRepository.findAllBy(pageable)).thenReturn(page);

    // When
    Page<Match> result = matchService.getAllMatches(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0)).isEqualTo(testMatch);
    verify(matchRepository).findAllBy(pageable);
  }

  @Test
  @DisplayName("Should get matches by show")
  void shouldGetMatchesByShow() {
    // Given
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByShow(testShow)).thenReturn(matches);

    // When
    List<Match> result = matchService.getMatchesByShow(testShow);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByShow(testShow);
  }

  @Test
  @DisplayName("Should get matches by wrestler participation")
  void shouldGetMatchesByWrestlerParticipation() {
    // Given
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByWrestlerParticipation(testWinner)).thenReturn(matches);

    // When
    List<Match> result =
        matchService.getMatchesByWrestlerParticipation(testWinner);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByWrestlerParticipation(testWinner);
  }

  @Test
  @DisplayName("Should get matches by winner")
  void shouldGetMatchesByWinner() {
    // Given
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByWinner(testWinner)).thenReturn(matches);

    // When
    List<Match> result = matchService.getMatchesByWinner(testWinner);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByWinner(testWinner);
  }

  @Test
  @DisplayName("Should get matches between two wrestlers")
  void shouldGetMatchesBetweenTwoWrestlers() {
    // Given
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Test Wrestler 2");

    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findMatchesBetween(testWinner, wrestler2)).thenReturn(matches);

    // When
    List<Match> result = matchService.getMatchesBetween(testWinner, wrestler2);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findMatchesBetween(testWinner, wrestler2);
  }

  @Test
  @DisplayName("Should get NPC generated matches")
  void shouldGetNpcGeneratedMatches() {
    // Given
    testMatch.setIsNpcGenerated(true);
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByIsNpcGeneratedTrue()).thenReturn(matches);

    // When
    List<Match> result = matchService.getNpcGeneratedMatches();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByIsNpcGeneratedTrue();
  }

  @Test
  @DisplayName("Should get title matches")
  void shouldGetTitleMatches() {
    // Given
    testMatch.setIsTitleMatch(true);
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByIsTitleMatchTrue()).thenReturn(matches);

    // When
    List<Match> result = matchService.getTitleMatches();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByIsTitleMatchTrue();
  }

  @Test
  @DisplayName("Should get matches after specific date")
  void shouldGetMatchesAfterSpecificDate() {
    // Given
    Instant afterDate = testDate.minusSeconds(3600);
    List<Match> matches = Arrays.asList(testMatch);
    when(matchRepository.findByMatchDateAfter(afterDate)).thenReturn(matches);

    // When
    List<Match> result = matchService.getMatchesAfter(afterDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testMatch);
    verify(matchRepository).findByMatchDateAfter(afterDate);
  }

  @Test
  @DisplayName("Should count wins by wrestler")
  void shouldCountWinsByWrestler() {
    // Given
    when(matchRepository.countWinsByWrestler(testWinner)).thenReturn(5L);

    // When
    long result = matchService.countWinsByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(5L);
    verify(matchRepository).countWinsByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should count total matches by wrestler")
  void shouldCountTotalMatchesByWrestler() {
    // Given
    when(matchRepository.countMatchesByWrestler(testWinner)).thenReturn(10L);

    // When
    long result = matchService.countMatchesByWrestler(testWinner);

    // Then
    assertThat(result).isEqualTo(10L);
    verify(matchRepository).countMatchesByWrestler(testWinner);
  }

  @Test
  @DisplayName("Should check if match exists by external ID")
  void shouldCheckIfMatchExistsByExternalId() {
    // Given
    when(matchRepository.existsByExternalId("notion-123")).thenReturn(true);

    // When
    boolean result = matchService.existsByExternalId("notion-123");

    // Then
    assertThat(result).isTrue();
    verify(matchRepository).existsByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should find match by external ID")
  void shouldFindMatchByExternalId() {
    // Given
    when(matchRepository.findByExternalId("notion-123"))
        .thenReturn(Optional.of(testMatch));

    // When
    Optional<Match> result = matchService.findByExternalId("notion-123");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testMatch);
    verify(matchRepository).findByExternalId("notion-123");
  }

  @Test
  @DisplayName("Should delete match by ID")
  void shouldDeleteMatchById() {
    // When
    matchService.deleteMatch(1L);

    // Then
    verify(matchRepository).deleteById(1L);
  }
}
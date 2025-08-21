package com.github.javydreamercsw.management.service.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for SeasonService. Tests the ATW RPG season management functionality. */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonService Tests")
class SeasonServiceTest {

  @Mock private SeasonRepository seasonRepository;

  private Clock fixedClock;
  private SeasonService seasonService;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    seasonService = new SeasonService(seasonRepository, fixedClock);
  }

  @Test
  @DisplayName("Should create new season with default values")
  void shouldCreateNewSeasonWithDefaultValues() {
    // Given
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());
    when(seasonRepository.findLatestSeason()).thenReturn(Optional.empty());
    when(seasonRepository.saveAndFlush(any(Season.class)))
        .thenAnswer(
            invocation -> {
              Season season = invocation.getArgument(0);
              season.setId(1L);
              return season;
            });

    // When
    Season result = seasonService.createSeason("Test Season", "Test description", null);

    // Then
    assertThat(result.getName()).isEqualTo("Test Season");
    assertThat(result.getDescription()).isEqualTo("Test description");
    assertThat(result.getSeasonNumber()).isEqualTo(1);
    assertThat(result.getShowsPerPpv()).isEqualTo(5); // Default value
    assertThat(result.getIsActive()).isTrue();
    verify(seasonRepository).saveAndFlush(any(Season.class));
  }

  @Test
  @DisplayName("Should create season with custom shows per PPV")
  void shouldCreateSeasonWithCustomShowsPerPpv() {
    // Given
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());
    when(seasonRepository.findLatestSeason()).thenReturn(Optional.empty());
    when(seasonRepository.saveAndFlush(any(Season.class)))
        .thenAnswer(
            invocation -> {
              Season season = invocation.getArgument(0);
              season.setId(1L);
              return season;
            });

    // When
    Season result = seasonService.createSeason("Test Season", "Test description", 4);

    // Then
    assertThat(result.getShowsPerPpv()).isEqualTo(4);
  }

  @Test
  @DisplayName("Should end active season when creating new one")
  void shouldEndActiveSeasonWhenCreatingNewOne() {
    // Given
    Season activeSeason = createSeason("Active Season", 1);
    activeSeason.setIsActive(true);

    when(seasonRepository.findActiveSeason()).thenReturn(Optional.of(activeSeason));
    when(seasonRepository.findLatestSeason()).thenReturn(Optional.of(activeSeason));
    when(seasonRepository.saveAndFlush(any(Season.class)))
        .thenAnswer(
            invocation -> {
              Season season = invocation.getArgument(0);
              if (season.getId() == null) season.setId(2L);
              return season;
            });

    // When
    Season result = seasonService.createSeason("New Season", "New description", null);

    // Then
    assertThat(activeSeason.getIsActive()).isFalse();
    assertThat(activeSeason.getEndDate()).isNotNull();
    assertThat(result.getSeasonNumber()).isEqualTo(2);
    verify(seasonRepository, times(2)).saveAndFlush(any(Season.class)); // End active + save new
  }

  @Test
  @DisplayName("Should get active season")
  void shouldGetActiveSeason() {
    // Given
    Season activeSeason = createSeason("Active Season", 1);
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.of(activeSeason));

    // When
    Optional<Season> result = seasonService.getActiveSeason();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(activeSeason);
  }

  @Test
  @DisplayName("Should get season by ID")
  void shouldGetSeasonById() {
    // Given
    Season season = createSeason("Test Season", 1);
    when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

    // When
    Optional<Season> result = seasonService.getSeasonById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(season);
  }

  @Test
  @DisplayName("Should end current season")
  void shouldEndCurrentSeason() {
    // Given
    Season activeSeason = createSeason("Active Season", 1);
    activeSeason.setIsActive(true);
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.of(activeSeason));
    when(seasonRepository.saveAndFlush(any(Season.class))).thenReturn(activeSeason);

    // When
    Optional<Season> result = seasonService.endCurrentSeason();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getIsActive()).isFalse();
    assertThat(result.get().getEndDate()).isNotNull();
    verify(seasonRepository).saveAndFlush(activeSeason);
  }

  @Test
  @DisplayName("Should add show to active season")
  void shouldAddShowToActiveSeason() {
    // Given
    Season activeSeason = createSeason("Active Season", 1);
    Show show = createShow("Test Show");

    when(seasonRepository.findActiveSeason()).thenReturn(Optional.of(activeSeason));
    when(seasonRepository.saveAndFlush(any(Season.class))).thenReturn(activeSeason);

    // When
    Optional<Season> result = seasonService.addShowToActiveSeason(show);

    // Then
    assertThat(result).isPresent();
    assertThat(activeSeason.getShows()).contains(show);
    assertThat(show.getSeason()).isEqualTo(activeSeason);
    verify(seasonRepository).saveAndFlush(activeSeason);
  }

  @Test
  @DisplayName("Should return empty when no active season for show addition")
  void shouldReturnEmptyWhenNoActiveSeasonForShowAddition() {
    // Given
    Show show = createShow("Test Show");
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());

    // When
    Optional<Season> result = seasonService.addShowToActiveSeason(show);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should check if time for PPV")
  void shouldCheckIfTimeForPpv() {
    // Given
    Season activeSeason = createSeason("Active Season", 1);
    activeSeason.setShowsPerPpv(3);

    // Add 3 regular shows
    for (int i = 1; i <= 3; i++) {
      Show show = createShow("Show " + i);
      activeSeason.addShow(show);
    }

    when(seasonRepository.findActiveSeason()).thenReturn(Optional.of(activeSeason));

    // When
    boolean result = seasonService.isTimeForPpv();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false for PPV check when no active season")
  void shouldReturnFalseForPpvCheckWhenNoActiveSeason() {
    // Given
    when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());

    // When
    boolean result = seasonService.isTimeForPpv();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should update season settings")
  void shouldUpdateSeasonSettings() {
    // Given
    Season season = createSeason("Old Name", 1);
    when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
    when(seasonRepository.saveAndFlush(any(Season.class))).thenReturn(season);

    // When
    Optional<Season> result = seasonService.updateSeason(1L, "New Name", "New description", 4);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Name");
    assertThat(result.get().getDescription()).isEqualTo("New description");
    assertThat(result.get().getShowsPerPpv()).isEqualTo(4);
    verify(seasonRepository).saveAndFlush(season);
  }

  @Test
  @DisplayName("Should delete inactive season with no shows")
  void shouldDeleteInactiveSeasonWithNoShows() {
    // Given
    Season season = createSeason("Test Season", 1);
    season.setIsActive(false);
    when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

    // When
    boolean result = seasonService.deleteSeason(1L);

    // Then
    assertThat(result).isTrue();
    verify(seasonRepository).delete(season);
  }

  @Test
  @DisplayName("Should not delete active season")
  void shouldNotDeleteActiveSeason() {
    // Given
    Season season = createSeason("Test Season", 1);
    season.setIsActive(true);
    when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

    // When
    boolean result = seasonService.deleteSeason(1L);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should get season statistics")
  void shouldGetSeasonStatistics() {
    // Given
    Season season = createSeason("Test Season", 1);
    season.setShowsPerPpv(3);

    // Add shows
    Show regularShow1 = createShow("Regular Show 1");
    Show regularShow2 = createShow("Regular Show 2");
    Show ppvShow = createShow("PPV Show");
    ppvShow.getType().setName("PPV Event");

    season.addShow(regularShow1);
    season.addShow(regularShow2);
    season.addShow(ppvShow);

    when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

    // When
    SeasonService.SeasonStats result = seasonService.getSeasonStats(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Test Season");
    assertThat(result.totalShows()).isEqualTo(3);
    assertThat(result.regularShows()).isEqualTo(2);
    assertThat(result.ppvShows()).isEqualTo(1);
    assertThat(result.isActive()).isTrue();
  }

  private Season createSeason(String name, Integer seasonNumber) {
    Season season = new Season();
    season.setId(seasonNumber.longValue());
    season.setName(name);
    season.setSeasonNumber(seasonNumber);
    season.setShowsPerPpv(5);
    season.setIsActive(true);
    season.setStartDate(Instant.now(fixedClock));
    return season;
  }

  private Show createShow(String name) {
    Show show = new Show();
    show.setName(name);
    show.setDescription("Test show");

    ShowType showType = new ShowType();
    showType.setName("Regular Show");
    showType.setDescription("Regular show type");
    show.setType(showType);

    return show;
  }
}

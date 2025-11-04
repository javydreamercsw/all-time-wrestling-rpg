package com.github.javydreamercsw.management.service.rivalry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.HeatChangeEvent;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Unit tests for RivalryService. Tests the ATW RPG rivalry and heat management functionality. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RivalryService Tests")
class RivalryServiceTest {

  @Mock private RivalryRepository rivalryRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Clock clock;
  @Mock private Random random;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private RivalryService rivalryService;

  @BeforeEach
  void setUp() {
    lenient().when(clock.instant()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
  }

  @Test
  @DisplayName("Should create new rivalry between wrestlers")
  void shouldCreateNewRivalryBetweenWrestlers() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(rivalryRepository.findActiveRivalryBetween(wrestler1, wrestler2))
        .thenReturn(Optional.empty());
    when(rivalryRepository.saveAndFlush(any(Rivalry.class)))
        .thenAnswer(
            invocation -> {
              Rivalry rivalry = invocation.getArgument(0);
              rivalry.setId(1L);
              return rivalry;
            });

    // When
    Optional<Rivalry> result = rivalryService.createRivalry(1L, 2L, "Test storyline");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getWrestler1()).isEqualTo(wrestler1);
    assertThat(result.get().getWrestler2()).isEqualTo(wrestler2);
    assertThat(result.get().getHeat()).isEqualTo(0);
    assertThat(result.get().getIsActive()).isTrue();
    assertThat(result.get().getStorylineNotes()).isEqualTo("Test storyline");
    verify(rivalryRepository).saveAndFlush(any(Rivalry.class));
  }

  @Test
  @DisplayName("Should return existing rivalry if already exists")
  void shouldReturnExistingRivalryIfAlreadyExists() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry existingRivalry = createRivalry(wrestler1, wrestler2, 10);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(rivalryRepository.findActiveRivalryBetween(wrestler1, wrestler2))
        .thenReturn(Optional.of(existingRivalry));

    // When
    Optional<Rivalry> result = rivalryService.createRivalry(1L, 2L, "Test storyline");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(existingRivalry);
  }

  @Test
  @DisplayName("Should add heat to rivalry")
  void shouldAddHeatToRivalry() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 5);

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    Optional<Rivalry> result = rivalryService.addHeat(1L, 3, "Backstage confrontation");

    // Then
    assertThat(result).isPresent();
    assertThat(rivalry.getHeat()).isEqualTo(8); // 5 + 3
    verify(rivalryRepository).saveAndFlush(rivalry);
  }

  @Test
  @DisplayName("Should add heat between wrestlers")
  void shouldAddHeatBetweenWrestlers() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 5);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(rivalryRepository.findActiveRivalryBetween(wrestler1, wrestler2))
        .thenReturn(Optional.of(rivalry));
    when(rivalryRepository.findById(rivalry.getId())).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    Optional<Rivalry> result =
        rivalryService.addHeatBetweenWrestlers(1L, 2L, 4, "Match interference");

    // Then
    assertThat(result).isPresent();
    assertThat(rivalry.getHeat()).isEqualTo(9); // 5 + 4
  }

  @Test
  @DisplayName("Should successfully resolve rivalry with high dice rolls")
  void shouldSuccessfullyResolveRivalryWithHighDiceRolls() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 25); // Eligible for resolution

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    ResolutionResult<Rivalry> result = rivalryService.attemptResolution(1L, 16, 15); // Total = 31

    // Then
    assertThat(result.resolved()).isTrue();
    assertThat(result.message()).isEqualTo("Rivalry resolved successfully");
    assertThat(result.roll1()).isEqualTo(16);
    assertThat(result.roll2()).isEqualTo(15);
    assertThat(result.totalRoll()).isEqualTo(31);
    assertThat(rivalry.getIsActive()).isFalse();
    verify(rivalryRepository).saveAndFlush(rivalry);
  }

  @Test
  @DisplayName("Should fail to resolve rivalry with low dice rolls")
  void shouldFailToResolveRivalryWithLowDiceRolls() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 25); // Eligible for resolution

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));

    // When
    ResolutionResult<Rivalry> result = rivalryService.attemptResolution(1L, 10, 15); // Total = 25

    // Then
    assertThat(result.resolved()).isFalse();
    assertThat(result.message()).isEqualTo("Resolution attempt failed");
    assertThat(result.totalRoll()).isEqualTo(25);
    assertThat(rivalry.getIsActive()).isTrue();
  }

  @Test
  @DisplayName("Should not allow resolution below 20 heat")
  void shouldNotAllowResolutionBelow20Heat() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 15); // Not eligible for resolution

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));

    // When
    ResolutionResult<Rivalry> result = rivalryService.attemptResolution(1L, 20, 20);

    // Then
    assertThat(result.resolved()).isFalse();
    assertThat(result.message()).contains("needs at least 20 heat");
  }

  @Test
  @DisplayName("Should end rivalry manually")
  void shouldEndRivalryManually() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 15);

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    Optional<Rivalry> result = rivalryService.endRivalry(1L, "Storyline concluded");

    // Then
    assertThat(result).isPresent();
    assertThat(rivalry.getIsActive()).isFalse();
    assertThat(rivalry.getEndedDate()).isNotNull();
    verify(rivalryRepository).saveAndFlush(rivalry);
  }

  @Test
  @DisplayName("Should get rivalries for wrestler")
  void shouldGetRivalriesForWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 1L);
    Rivalry rivalry1 = createRivalry(wrestler, createWrestler("Opponent 1", 2L), 10);
    Rivalry rivalry2 = createRivalry(wrestler, createWrestler("Opponent 2", 3L), 15);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(rivalryRepository.findActiveRivalriesForWrestler(wrestler))
        .thenReturn(Arrays.asList(rivalry1, rivalry2));

    // When
    List<Rivalry> result = rivalryService.getRivalriesForWrestler(1L);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(rivalry1, rivalry2);
  }

  @Test
  @DisplayName("Should get rivalries requiring matches")
  void shouldGetRivalriesRequiringMatches() {
    // Given
    Rivalry rivalry1 = createRivalry(createWrestler("W1", 1L), createWrestler("W2", 2L), 12);
    Rivalry rivalry2 = createRivalry(createWrestler("W3", 3L), createWrestler("W4", 4L), 8);

    when(rivalryRepository.findRivalriesRequiringMatches()).thenReturn(List.of(rivalry1));

    // When
    List<Rivalry> result = rivalryService.getRivalriesRequiringMatches();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result).contains(rivalry1);
  }

  @Test
  @DisplayName("Should get rivalries by intensity")
  void shouldGetRivalriesByIntensity() {
    // Given
    when(rivalryRepository.findByHeatRange(10, 19)).thenReturn(List.of());

    // When
    List<Rivalry> result = rivalryService.getRivalriesByIntensity(RivalryIntensity.HEATED);

    // Then
    verify(rivalryRepository).findByHeatRange(10, 19);
  }

  @Test
  @DisplayName("Should update storyline notes")
  void shouldUpdateStorylineNotes() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 10);

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    Optional<Rivalry> result = rivalryService.updateStorylineNotes(1L, "Updated storyline");

    // Then
    assertThat(result).isPresent();
    assertThat(rivalry.getStorylineNotes()).isEqualTo("Updated storyline");
    verify(rivalryRepository).saveAndFlush(rivalry);
  }

  @Test
  @DisplayName("Should get rivalry statistics")
  void shouldGetRivalryStatistics() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 25);

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));

    // When
    RivalryService.RivalryStats result = rivalryService.getRivalryStats(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.wrestler1Name()).isEqualTo("Wrestler 1");
    assertThat(result.wrestler2Name()).isEqualTo("Wrestler 2");
    assertThat(result.heat()).isEqualTo(25);
    assertThat(result.intensity()).isEqualTo(RivalryIntensity.INTENSE);
    assertThat(result.canAttemptResolution()).isTrue();
    assertThat(result.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should check rivalry history")
  void shouldCheckRivalryHistory() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(rivalryRepository.hasRivalryHistory(wrestler1, wrestler2)).thenReturn(true);

    // When
    boolean result = rivalryService.hasRivalryHistory(1L, 2L);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should publish HeatChangeEvent with wrestlers")
  void shouldPublishHeatChangeEventWithWrestlers() {
    // Given
    Wrestler wrestler1 = createWrestler("Wrestler 1", 1L);
    Wrestler wrestler2 = createWrestler("Wrestler 2", 2L);
    Rivalry rivalry = createRivalry(wrestler1, wrestler2, 5);

    when(rivalryRepository.findById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryRepository.saveAndFlush(any(Rivalry.class))).thenReturn(rivalry);

    // When
    rivalryService.addHeat(1L, 3, "Backstage confrontation");

    // Then
    verify(eventPublisher)
        .publishEvent(
            argThat(
                event ->
                    event instanceof HeatChangeEvent
                        && ((HeatChangeEvent) event).getSource() == rivalryService
                        && ((HeatChangeEvent) event).getRivalryId() == rivalry.getId()
                        && ((HeatChangeEvent) event).getOldHeat() == 5
                        && ((HeatChangeEvent) event).getReason().equals("Backstage confrontation")
                        && ((HeatChangeEvent) event)
                            .getWrestlers()
                            .containsAll(List.of(wrestler1, wrestler2))
                        && List.of(wrestler1, wrestler2)
                            .containsAll(((HeatChangeEvent) event).getWrestlers())));
  }

  private Wrestler createWrestler(@NonNull String name, @NonNull Long id) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(id);
    wrestler.setName(name);
    wrestler.setFans(50000L);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    return wrestler;
  }

  private Rivalry createRivalry(
      @NonNull Wrestler wrestler1, @NonNull Wrestler wrestler2, int heat) {
    Rivalry rivalry = new Rivalry();
    rivalry.setId(1L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(heat);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now(clock));
    return rivalry;
  }
}

package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for InjuryService. Tests the ATW RPG injury management functionality. */
@ExtendWith(MockitoExtension.class)
@DisplayName("InjuryService Tests")
class InjuryServiceTest {
  @Mock private InjuryRepository injuryRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Clock clock;
  @Mock private Random random;
  @InjectMocks private InjuryService injuryService;

  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    lenient().when(clock.instant()).thenReturn(fixedClock.instant());
    lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());
  }

  @Test
  @DisplayName("Should create new injury for wrestler")
  void shouldCreateNewInjuryForWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(injuryRepository.saveAndFlush(any(Injury.class)))
        .thenAnswer(
            invocation -> {
              Injury injury = invocation.getArgument(0);
              injury.setId(1L);
              return injury;
            });

    // When
    Optional<Injury> result =
        injuryService.createInjury(
            1L, "Knee Injury", "Torn ACL", InjurySeverity.SEVERE, "Occurred during segment");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Knee Injury");
    assertThat(result.get().getDescription()).isEqualTo("Torn ACL");
    assertThat(result.get().getSeverity()).isEqualTo(InjurySeverity.SEVERE);
    assertThat(result.get().getWrestler()).isEqualTo(wrestler);
    assertThat(result.get().getIsActive()).isTrue();
    verify(injuryRepository).saveAndFlush(any(Injury.class));
  }

  @Test
  @DisplayName("Should create injury from bumps")
  void shouldCreateInjuryFromBumps() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    wrestler.setBumps(3); // Set bumps to trigger injury creation
    when(wrestlerRepository.findById(wrestler.getId())).thenReturn(Optional.of(wrestler));

    when(injuryRepository.saveAndFlush(any(Injury.class)))
        .thenAnswer(
            invocation -> {
              Injury injury = invocation.getArgument(0);
              injury.setId(1L);
              return injury;
            });

    // When
    Optional<Injury> result = injuryService.createInjuryFromBumps(wrestler.getId());

    // Then
    assertThat(result).isNotNull();
    Assertions.assertTrue(result.isPresent());
    assertThat(result.get().getWrestler()).isEqualTo(wrestler);
    assertThat(result.get().getIsActive()).isTrue();
    assertThat(wrestler.getInjuries()).contains(result.get());
    verify(injuryRepository).saveAndFlush(any(Injury.class));
  }

  @Test
  @DisplayName("Should successfully heal injury with good dice roll")
  void shouldSuccessfullyHealInjuryWithGoodDiceRoll() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);

    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(wrestler);
    when(injuryRepository.saveAndFlush(any(Injury.class))).thenReturn(injury);

    // When - Roll 6 (should succeed for MINOR injury with threshold 3)
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 6);

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.message()).isEqualTo("Injury healed successfully");
    assertThat(result.diceRoll()).isEqualTo(6);
    assertThat(result.fansSpent()).isTrue();
    assertThat(injury.getIsActive()).isFalse();
    assertThat(wrestler.getFans()).isEqualTo(45000L); // 50k - 5k healing cost
    verify(wrestlerRepository).saveAndFlush(wrestler);
    verify(injuryRepository).saveAndFlush(injury);
  }

  @Test
  @DisplayName("Should fail to heal injury with bad dice roll")
  void shouldFailToHealInjuryWithBadDiceRoll() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);

    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(wrestler);

    // When - Roll 2 (should fail for MINOR injury with threshold 3)
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 2);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).isEqualTo("Healing attempt failed");
    assertThat(result.diceRoll()).isEqualTo(2);
    assertThat(result.fansSpent()).isTrue();
    assertThat(injury.getIsActive()).isTrue(); // Still active
    assertThat(wrestler.getFans()).isEqualTo(45000L); // Fans still spent
    verify(wrestlerRepository).saveAndFlush(wrestler);
  }

  @Test
  @DisplayName("Should fail healing when wrestler cannot afford cost")
  void shouldFailHealingWhenWrestlerCannotAffordCost() {
    // Given
    Wrestler wrestler = createWrestler("Poor Wrestler", 1000L); // Only 1k fans
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR); // Costs 5k to heal

    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));

    // When
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 6);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("cannot afford");
    assertThat(result.fansSpent()).isFalse();
  }

  @Test
  @DisplayName("Should fail healing when injury cannot be healed")
  void shouldFailHealingWhenInjuryCannotBeHealed() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);
    injury.heal(); // Already healed

    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));

    // When
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 6);

    // Then
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("cannot be healed");
    assertThat(result.fansSpent()).isFalse();
  }

  @Test
  @DisplayName("Should get active injuries for wrestler")
  void shouldGetActiveInjuriesForWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury1 = createInjury(wrestler, InjurySeverity.MINOR);
    Injury injury2 = createInjury(wrestler, InjurySeverity.MODERATE);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(injuryRepository.findActiveInjuriesForWrestler(wrestler))
        .thenReturn(Arrays.asList(injury1, injury2));

    // When
    List<Injury> result = injuryService.getActiveInjuriesForWrestler(1L);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(injury1, injury2);
  }

  @Test
  @DisplayName("Should get total health penalty for wrestler")
  void shouldGetTotalHealthPenaltyForWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(injuryRepository.getTotalHealthPenaltyForWrestler(wrestler)).thenReturn(5);

    // When
    Integer result = injuryService.getTotalHealthPenaltyForWrestler(1L);

    // Then
    assertThat(result).isEqualTo(5);
  }

  @Test
  @DisplayName("Should get injury statistics for wrestler")
  void shouldGetInjuryStatisticsForWrestler() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    wrestler.setStartingHealth(15);

    Injury activeInjury = createInjury(wrestler, InjurySeverity.MINOR);
    Injury healedInjury = createInjury(wrestler, InjurySeverity.MODERATE);
    healedInjury.heal();

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(injuryRepository.findActiveInjuriesForWrestler(wrestler))
        .thenReturn(List.of(activeInjury));
    when(injuryRepository.findByWrestler(wrestler))
        .thenReturn(Arrays.asList(activeInjury, healedInjury));
    when(injuryRepository.getTotalHealthPenaltyForWrestler(wrestler)).thenReturn(2);

    // When
    InjuryService.InjuryStats result = injuryService.getInjuryStatsForWrestler(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.wrestlerName()).isEqualTo("Test Wrestler");
    assertThat(result.activeInjuries()).isEqualTo(1);
    assertThat(result.healedInjuries()).isEqualTo(1);
    assertThat(result.totalHealthPenalty()).isEqualTo(2);
    assertThat(result.effectiveHealth()).isEqualTo(15); // Already includes penalty calculation
  }

  @Test
  @DisplayName("Should update injury information")
  void shouldUpdateInjuryInformation() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);

    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    when(injuryRepository.saveAndFlush(any(Injury.class))).thenReturn(injury);

    // When
    Optional<Injury> result =
        injuryService.updateInjury(1L, "Updated Name", "Updated description", "Updated notes");

    // Then
    assertThat(result).isPresent();
    assertThat(injury.getName()).isEqualTo("Updated Name");
    assertThat(injury.getDescription()).isEqualTo("Updated description");
    assertThat(injury.getInjuryNotes()).isEqualTo("Updated notes");
    verify(injuryRepository).saveAndFlush(injury);
  }

  @Test
  @DisplayName("Should get injuries by severity")
  void shouldGetInjuriesBySeverity() {
    // Given
    Injury injury1 = createInjury(createWrestler("W1", 50000L), InjurySeverity.SEVERE);
    Injury injury2 = createInjury(createWrestler("W2", 50000L), InjurySeverity.SEVERE);

    when(injuryRepository.findBySeverity(InjurySeverity.SEVERE))
        .thenReturn(Arrays.asList(injury1, injury2));

    // When
    List<Injury> result = injuryService.getInjuriesBySeverity(InjurySeverity.SEVERE);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(injury1, injury2);
  }

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName(name);
    wrestler.setFans(fans);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    wrestler.updateTier();
    return wrestler;
  }

  private Injury createInjury(Wrestler wrestler, InjurySeverity severity) {
    Injury injury = new Injury();
    injury.setId(1L);
    injury.setWrestler(wrestler);
    injury.setName("Test Injury");
    injury.setDescription("Test injury description");
    injury.setSeverity(severity);
    injury.setHealthPenalty(severity.getRandomHealthPenalty());
    injury.setHealingCost(severity.getBaseHealingCost());
    injury.setIsActive(true);
    injury.setInjuryDate(Instant.now(fixedClock));
    return injury;
  }
}

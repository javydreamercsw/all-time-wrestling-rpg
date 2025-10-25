package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    when(random.nextInt(any(int.class))).thenReturn(1); // Control random for health penalty

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
    assertThat(result.get().getHealthPenalty()).isEqualTo(4); // SEVERE min is 3, so 3 + 1 = 4
    verify(injuryRepository).saveAndFlush(any(Injury.class));
  }

  @ParameterizedTest(name = "Tier {0}, Roll {1} -> Severity {2}")
  @CsvSource({
    "ROOKIE, 34, MINOR",
    "ROOKIE, 64, MODERATE",
    "ROOKIE, 89, SEVERE",
    "ROOKIE, 99, CRITICAL",
    "RISER, 39, MINOR",
    "RISER, 69, MODERATE",
    "RISER, 91, SEVERE",
    "RISER, 99, CRITICAL",
    "CONTENDER, 44, MINOR",
    "CONTENDER, 74, MODERATE",
    "CONTENDER, 93, SEVERE",
    "CONTENDER, 99, CRITICAL",
    "MIDCARDER, 54, MINOR",
    "MIDCARDER, 79, MODERATE",
    "MIDCARDER, 95, SEVERE",
    "MIDCARDER, 99, CRITICAL",
    "MAIN_EVENTER, 59, MINOR",
    "MAIN_EVENTER, 84, MODERATE",
    "MAIN_EVENTER, 96, SEVERE",
    "MAIN_EVENTER, 99, CRITICAL",
    "ICON, 64, MINOR",
    "ICON, 87, MODERATE",
    "ICON, 97, SEVERE",
    "ICON, 99, CRITICAL"
  })
  @DisplayName("Should create injury from bumps with correct severity based on tier and roll")
  void testCreateInjuryFromBumpsSeverity(
      WrestlerTier tier, int roll, InjurySeverity expectedSeverity) {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    wrestler.setTier(tier);
    wrestler.setBumps(3);
    when(wrestlerRepository.findById(wrestler.getId())).thenReturn(Optional.of(wrestler));
    when(random.nextInt(100)).thenReturn(roll - 1); // Control the d100 roll
    when(random.nextInt(4)).thenReturn(0); // For injury name generation

    when(injuryRepository.saveAndFlush(any(Injury.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Optional<Injury> result = injuryService.createInjuryFromBumps(wrestler.getId());

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedSeverity, result.get().getSeverity());
  }

  @Test
  @DisplayName("Should attempt healing with a random roll")
  void shouldAttemptHealingWithRandomRoll() {
    // Given
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);
    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(wrestler);
    when(random.nextInt(6)).thenReturn(5); // Roll a 6 (5+1)

    // When
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, null);

    // Then
    assertTrue(result.success());
    assertEquals(6, result.diceRoll());
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
    assertTrue(result.isPresent());
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
  @DisplayName("Should return empty when healing injury not found")
  void shouldReturnEmptyWhenHealingInjuryNotFound() {
    when(injuryRepository.findById(99L)).thenReturn(Optional.empty());
    InjuryService.HealingResult result = injuryService.attemptHealing(99L, 3);
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("Injury not found");
  }

  @Test
  @DisplayName("Should return failure when injury cannot be healed")
  void shouldReturnFailureWhenInjuryCannotBeHealed() {
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);
    injury.heal(); // Mark as healed
    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 3);
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("cannot be healed");
  }

  @Test
  @DisplayName("Should return failure when wrestler cannot afford healing cost")
  void shouldReturnFailureWhenWrestlerCannotAffordHealingCost() {
    Wrestler wrestler = createWrestler("Test Wrestler", 0L); // No fans
    Injury injury = createInjury(wrestler, InjurySeverity.MINOR);
    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    InjuryService.HealingResult result = injuryService.attemptHealing(1L, 3);
    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("cannot afford");
  }

  @Test
  @DisplayName("Should get injury by ID")
  void shouldGetInjuryById() {
    Injury injury = new Injury();
    injury.setId(1L);
    when(injuryRepository.findById(1L)).thenReturn(Optional.of(injury));
    Optional<Injury> result = injuryService.getInjuryById(1L);
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should get all injuries with pagination")
  void shouldGetAllInjuriesWithPagination() {
    Injury injury = new Injury();
    when(injuryRepository.findAllBy(any()))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(injury)));
    var result =
        injuryService.getAllInjuries(org.springframework.data.domain.PageRequest.of(0, 10));
    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("Should get active injuries for wrestler")
  void shouldGetActiveInjuriesForWrestler() {
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    Injury injury = new Injury();
    when(injuryRepository.findActiveInjuriesForWrestler(wrestler)).thenReturn(List.of(injury));
    List<Injury> result = injuryService.getActiveInjuriesForWrestler(1L);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("Should get all injuries for wrestler")
  void shouldGetAllInjuriesForWrestler() {
    Wrestler wrestler = createWrestler("Test Wrestler", 50000L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    Injury injury = new Injury();
    when(injuryRepository.findByWrestler(wrestler)).thenReturn(List.of(injury));
    List<Injury> result = injuryService.getAllInjuriesForWrestler(1L);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("Should get injuries by severity")
  void shouldGetInjuriesBySeverity() {
    Injury injury = new Injury();
    when(injuryRepository.findBySeverity(InjurySeverity.MINOR)).thenReturn(List.of(injury));
    List<Injury> result = injuryService.getInjuriesBySeverity(InjurySeverity.MINOR);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("Should get all active injuries")
  void shouldGetAllActiveInjuries() {
    Injury injury = new Injury();
    when(injuryRepository.findAllActiveInjuries()).thenReturn(List.of(injury));
    List<Injury> result = injuryService.getAllActiveInjuries();
    assertThat(result).hasSize(1);
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

  private Wrestler createWrestler(String name, Long fans) {
    Wrestler wrestler = Wrestler.builder().build();
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
    injury.setHealthPenalty(severity.getRandomHealthPenalty(random));
    injury.setHealingCost(severity.getBaseHealingCost());
    injury.setIsActive(true);
    injury.setInjuryDate(Instant.now(fixedClock));
    return injury;
  }
}

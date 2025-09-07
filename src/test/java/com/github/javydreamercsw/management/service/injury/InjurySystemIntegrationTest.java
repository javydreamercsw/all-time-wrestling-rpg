package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.NPCMatchResolutionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the complete injury system including bump conversion, match integration,
 * and health calculations.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Injury System Integration Tests")
class InjurySystemIntegrationTest {

  @Autowired private InjuryService injuryService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private NPCMatchResolutionService npcMatchResolutionService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private MatchTypeRepository matchTypeRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  @PersistenceContext private EntityManager entityManager;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Show testShow;
  private MatchType singlesMatchType;

  @BeforeEach
  void setUp() {
    // Clean up
    matchRepository.deleteAll();
    injuryRepository.deleteAll();
    wrestlerRepository.deleteAll();
    showRepository.deleteAll();
    showTypeRepository.deleteAll();
    matchTypeRepository.deleteAll();

    // Create test data
    wrestler1 = wrestlerService.createWrestler("Test Wrestler 1", true, null);
    wrestler1.setFans(25000L); // Riser tier
    wrestler1.updateTier();
    wrestler1 = wrestlerRepository.save(wrestler1);

    wrestler2 = wrestlerService.createWrestler("Test Wrestler 2", true, null);
    wrestler2.setFans(25000L); // Riser tier
    wrestler2.updateTier();
    wrestler2 = wrestlerRepository.save(wrestler2);

    // Create show type and show
    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test show type for integration tests");
    showType = showTypeRepository.save(showType);

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test show for integration tests");
    testShow.setType(showType);
    testShow.setShowDate(java.time.LocalDate.now());
    testShow = showRepository.save(testShow);

    // Create match type
    singlesMatchType = new MatchType();
    singlesMatchType.setName("Singles Match");
    singlesMatchType.setDescription("One-on-one wrestling match");
    singlesMatchType = matchTypeRepository.save(singlesMatchType);
  }

  @Test
  @DisplayName("Should convert 3 bumps to injury and reset bumps")
  void shouldConvert3BumpsToInjuryAndResetBumps() {
    // Given - Add 2 bumps first
    Optional<Wrestler> afterFirstBump = wrestlerService.addBump(wrestler1.getId());
    assertThat(afterFirstBump).isPresent();
    assertThat(afterFirstBump.get().getBumps()).isEqualTo(1);

    Optional<Wrestler> afterSecondBump = wrestlerService.addBump(wrestler1.getId());
    assertThat(afterSecondBump).isPresent();
    assertThat(afterSecondBump.get().getBumps()).isEqualTo(2);

    // When - Add third bump (should trigger injury)
    Optional<Wrestler> afterThirdBump = wrestlerService.addBump(wrestler1.getId());

    // Then - Bumps should be reset and injury created
    assertThat(afterThirdBump).isPresent();
    assertThat(afterThirdBump.get().getBumps()).isEqualTo(0);

    // Force transaction commit and refresh
    entityManager.flush();
    entityManager.clear();

    List<Injury> injuries = injuryRepository.findActiveInjuriesForWrestler(wrestler1);
    assertThat(injuries).hasSize(1);
    assertThat(injuries.get(0).getInjuryNotes()).contains("Generated from bump accumulation");
  }

  @Test
  @DisplayName("Should apply injury penalties to health calculations")
  void shouldApplyInjuryPenaltiesToHealthCalculations() {
    // Given - Create injury for wrestler
    Optional<Injury> createdInjury =
        injuryService.createInjury(
            wrestler1.getId(),
            "Test Injury",
            "Test injury for health calculation",
            InjurySeverity.MODERATE,
            "Test injury");

    assertThat(createdInjury).isPresent();

    // When - Check health calculations using injury service
    List<Injury> activeInjuries = injuryRepository.findActiveInjuriesForWrestler(wrestler1);
    assertThat(activeInjuries).hasSize(1);

    Integer totalPenalty = activeInjuries.stream().mapToInt(Injury::getHealthPenalty).sum();

    // Then - Injury penalty should be applied
    assertThat(totalPenalty).isGreaterThan(0);
    assertThat(createdInjury.get().getHealthPenalty()).isEqualTo(totalPenalty);
  }

  @Test
  @DisplayName("Should calculate injury statistics correctly")
  void shouldCalculateInjuryStatisticsCorrectly() {
    // Given - Create multiple injuries for wrestler
    injuryService.createInjury(
        wrestler1.getId(), "Active Injury 1", "First active injury", InjurySeverity.MINOR, "Test");

    injuryService.createInjury(
        wrestler1.getId(),
        "Active Injury 2",
        "Second active injury",
        InjurySeverity.MODERATE,
        "Test");

    // Create and heal one injury
    Injury healedInjury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                "Healed Injury",
                "This injury will be healed",
                InjurySeverity.SEVERE,
                "Test")
            .orElseThrow();

    healedInjury.heal();
    injuryRepository.save(healedInjury);

    // When - Get injury statistics
    InjuryService.InjuryStats stats = injuryService.getInjuryStatsForWrestler(wrestler1.getId());

    // Then - Statistics should be accurate
    assertThat(stats).isNotNull();
    assertThat(stats.wrestlerName()).isEqualTo("Test Wrestler 1");
    assertThat(stats.activeInjuries()).isEqualTo(2);
    assertThat(stats.healedInjuries()).isEqualTo(1);
    assertThat(stats.totalHealthPenalty()).isGreaterThan(0);
    assertThat(stats.effectiveHealth()).isLessThanOrEqualTo(wrestler1.getStartingHealth());
    assertThat(stats.totalHealingCost()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should handle injury healing with fan cost")
  void shouldHandleInjuryHealingWithFanCost() {
    // Given - Create injury and ensure wrestler has enough fans
    wrestler1.setFans(50000L); // Plenty of fans
    wrestler1 = wrestlerRepository.save(wrestler1);

    Injury injury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                "Healable Injury",
                "This injury can be healed",
                InjurySeverity.MINOR,
                "Test")
            .orElseThrow();

    Long originalFans = wrestler1.getFans();
    Long healingCost = injury.getHealingCost();

    // When - Attempt healing with good dice roll
    InjuryService.HealingResult result = injuryService.attemptHealing(injury.getId(), 6);

    // Then - Healing should succeed and fans should be spent
    assertThat(result.success()).isTrue();
    assertThat(result.fansSpent()).isTrue();

    wrestler1 = wrestlerRepository.findById(wrestler1.getId()).orElseThrow();
    assertThat(wrestler1.getFans()).isEqualTo(originalFans - healingCost);

    injury = injuryRepository.findById(injury.getId()).orElseThrow();
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("Should prevent healing when wrestler cannot afford cost")
  void shouldPreventHealingWhenWrestlerCannotAffordCost() {
    // Given - Create injury and ensure wrestler has insufficient fans
    wrestler1.setFans(1000L); // Not enough fans
    wrestler1 = wrestlerRepository.save(wrestler1);

    Injury injury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                "Expensive Injury",
                "This injury is too expensive to heal",
                InjurySeverity.SEVERE, // Expensive to heal
                "Test")
            .orElseThrow();

    Long originalFans = wrestler1.getFans();

    // When - Attempt healing
    InjuryService.HealingResult result = injuryService.attemptHealing(injury.getId(), 6);

    // Then - Healing should fail and no fans should be spent
    assertThat(result.success()).isFalse();
    assertThat(result.fansSpent()).isFalse();
    assertThat(result.message()).contains("cannot afford");

    wrestler1 = wrestlerRepository.findById(wrestler1.getId()).orElseThrow();
    assertThat(wrestler1.getFans()).isEqualTo(originalFans);

    injury = injuryRepository.findById(injury.getId()).orElseThrow();
    assertThat(injury.isCurrentlyActive()).isTrue();
  }
}

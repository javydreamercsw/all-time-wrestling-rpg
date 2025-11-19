package com.github.javydreamercsw.management.domain.injury;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for Injury entity. Tests the ATW RPG injury system functionality. */
@DisplayName("Injury Tests")
class InjuryTest {

  private Injury injury;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setFans(50000L);
    wrestler.setStartingHealth(15);

    injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setName("Knee Injury");
    injury.setDescription("Torn ACL from high-impact move");
    injury.setSeverity(InjurySeverity.MODERATE);
    injury.setHealthPenalty(3);
    injury.setHealingCost(10000L);
    injury.setInjuryDate(Instant.now()); // Initialize injury date
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Injury newInjury = new Injury();

    assertThat(newInjury.getIsActive()).isTrue();
    assertThat(newInjury.getHealedDate()).isNull();
    assertThat(newInjury.getHealingCost()).isEqualTo(10000L);
  }

  @Test
  @DisplayName("Should check if currently active")
  void shouldCheckIfCurrentlyActive() {
    // Active injury with no healed date
    assertThat(injury.isCurrentlyActive()).isTrue();

    // Inactive injury
    injury.setIsActive(false);
    assertThat(injury.isCurrentlyActive()).isFalse();

    // Active but healed injury
    injury.setIsActive(true);
    injury.setHealedDate(Instant.now());
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("Should heal injury properly")
  void shouldHealInjuryProperly() {
    assertThat(injury.isCurrentlyActive()).isTrue();

    Instant beforeHeal = Instant.now();
    injury.heal();
    Instant afterHeal = Instant.now();

    assertThat(injury.getIsActive()).isFalse();
    assertThat(injury.getHealedDate()).isBetween(beforeHeal, afterHeal);
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("Should calculate days active")
  void shouldCalculateDaysActive() {
    // Same day injury
    assertThat(injury.getDaysActive()).isEqualTo(0);

    // Injury from 5 days ago
    injury.setInjuryDate(Instant.now().minusSeconds(5 * 24 * 60 * 60));
    assertThat(injury.getDaysActive()).isEqualTo(5);

    // Healed injury (should use healed date)
    Instant healedDate = injury.getInjuryDate().plusSeconds(3 * 24 * 60 * 60);
    injury.setHealedDate(healedDate);
    assertThat(injury.getDaysActive()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should create display string")
  void shouldCreateDisplayString() {
    injury.setName("Shoulder Injury");
    injury.setSeverity(InjurySeverity.SEVERE);
    injury.setHealthPenalty(4);

    // Active injury
    String display = injury.getDisplayString();
    assertThat(display).isEqualTo("Shoulder Injury - Severe (4 health penalty) (Active)");

    // Healed injury
    injury.heal();
    display = injury.getDisplayString();
    assertThat(display).isEqualTo("Shoulder Injury - Severe (4 health penalty) (Healed)");
  }

  @Test
  @DisplayName("Should show status emojis")
  void shouldShowStatusEmojis() {
    // Active injury
    injury.setSeverity(InjurySeverity.MODERATE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🟠");

    injury.setSeverity(InjurySeverity.SEVERE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🔴");

    // Healed injury
    injury.heal();
    assertThat(injury.getStatusEmoji()).isEqualTo("✅");
  }

  @Test
  @DisplayName("Should calculate health impact")
  void shouldCalculateHealthImpact() {
    injury.setHealthPenalty(3);

    // Active injury
    assertThat(injury.getHealthImpact()).isEqualTo(3);

    // Healed injury
    injury.heal();
    assertThat(injury.getHealthImpact()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should check if can be healed")
  void shouldCheckIfCanBeHealed() {
    // Active injury can be healed
    assertThat(injury.canBeHealed()).isTrue();

    // Healed injury cannot be healed again
    injury.heal();
    assertThat(injury.canBeHealed()).isFalse();

    // Inactive injury cannot be healed
    injury.setIsActive(false);
    injury.setHealedDate(null);
    assertThat(injury.canBeHealed()).isFalse();
  }

  @Test
  @DisplayName("Should get healing fan cost")
  void shouldGetHealingFanCost() {
    injury.setHealingCost(15000L);
    assertThat(injury.getHealingFanCost()).isEqualTo(15000L);
  }

  @Test
  @DisplayName("Should format duration display")
  void shouldFormatDurationDisplay() {
    // Less than 1 day
    assertThat(injury.getDurationDisplay()).isEqualTo("Less than 1 day");

    // Exactly 1 day
    injury.setInjuryDate(Instant.now().minusSeconds(24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("1 day");

    // Multiple days
    injury.setInjuryDate(Instant.now().minusSeconds(3 * 24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("3 days");

    // 1 week
    injury.setInjuryDate(Instant.now().minusSeconds(7 * 24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("1 week");

    // Multiple weeks
    injury.setInjuryDate(Instant.now().minusSeconds(14 * 24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("2 weeks");

    // 1 month
    injury.setInjuryDate(Instant.now().minusSeconds(30 * 24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("1 month");

    // Multiple months
    injury.setInjuryDate(Instant.now().minusSeconds(60 * 24 * 60 * 60));
    assertThat(injury.getDurationDisplay()).isEqualTo("2 months");
  }

  @Test
  @DisplayName("Should maintain relationship with wrestler")
  void shouldMaintainRelationshipWithWrestler() {
    assertThat(injury.getWrestler()).isEqualTo(wrestler);
    assertThat(injury.getWrestler().getName()).isEqualTo("Test Wrestler");
  }

  @Test
  @DisplayName("Should handle different severity levels")
  void shouldHandleDifferentSeverityLevels() {
    injury.setSeverity(InjurySeverity.MINOR);
    assertThat(injury.getStatusEmoji()).isEqualTo("🟡");

    injury.setSeverity(InjurySeverity.MODERATE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🟠");

    injury.setSeverity(InjurySeverity.SEVERE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🔴");

    injury.setSeverity(InjurySeverity.CRITICAL);
    assertThat(injury.getStatusEmoji()).isEqualTo("💀");
  }

  @Test
  @DisplayName("Should handle injury notes")
  void shouldHandleInjuryNotes() {
    String notes = "Injury occurred during steel cage segment. Requires surgery.";
    injury.setInjuryNotes(notes);

    assertThat(injury.getInjuryNotes()).isEqualTo(notes);
  }

  @Test
  @DisplayName("Should handle very short injury duration")
  void shouldHandleVeryShortInjuryDuration() {
    // Injury that lasts only a few hours
    Instant start = Instant.now().minusSeconds(6 * 60 * 60); // 6 hours ago
    injury.setInjuryDate(start);
    injury.heal();

    assertThat(injury.getDaysActive()).isEqualTo(0);
    assertThat(injury.getDurationDisplay()).isEqualTo("Less than 1 day");
  }

  @Test
  @DisplayName("Should handle very long injury duration")
  void shouldHandleVeryLongInjuryDuration() {
    // Injury that lasts over a year
    injury.setInjuryDate(Instant.now().minusSeconds(400 * 24 * 60 * 60)); // 400 days ago

    assertThat(injury.getDaysActive()).isEqualTo(400);

    String display = injury.getDurationDisplay();
    assertThat(display).contains("month"); // Should show in months
  }

  @Test
  @DisplayName("Should handle healing already healed injury")
  void shouldHandleHealingAlreadyHealedInjury() {
    // Heal injury first time
    injury.heal();
    Instant firstHealDate = injury.getHealedDate();
    boolean firstActiveState = injury.getIsActive();

    // Heal again (should not change anything)
    injury.heal();

    // Should maintain the same state (healed date might be updated, but active state should remain)
    assertThat(injury.getIsActive()).isEqualTo(firstActiveState);
    assertThat(injury.getIsActive()).isFalse();
    assertThat(injury.getHealedDate()).isNotNull(); // Should still have a healed date
  }
}

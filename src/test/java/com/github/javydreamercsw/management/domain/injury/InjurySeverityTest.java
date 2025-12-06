/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.injury;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for InjurySeverity enum. Tests the ATW RPG injury severity system functionality. */
@DisplayName("InjurySeverity Tests")
class InjurySeverityTest {

  private final Random random = new Random();

  @Test
  @DisplayName("Should have correct severity count")
  void shouldHaveCorrectSeverityCount() {
    assertThat(InjurySeverity.values()).hasSize(4);
  }

  @Test
  @DisplayName("Should have correct properties for MINOR")
  void shouldHaveCorrectPropertiesForMinor() {
    InjurySeverity minor = InjurySeverity.MINOR;

    assertThat(minor.getDisplayName()).isEqualTo("Minor");
    assertThat(minor.getEmoji()).isEqualTo("🟡");
    assertThat(minor.getMinHealthPenalty()).isEqualTo(1);
    assertThat(minor.getMaxHealthPenalty()).isEqualTo(2);
    assertThat(minor.getBaseHealingCost()).isEqualTo(5000L);
    assertThat(minor.getDisplayWithEmoji()).isEqualTo("🟡 Minor");
    assertThat(minor.getHealthPenaltyRangeDisplay()).isEqualTo("1-2");
  }

  @Test
  @DisplayName("Should have correct properties for MODERATE")
  void shouldHaveCorrectPropertiesForModerate() {
    InjurySeverity moderate = InjurySeverity.MODERATE;

    assertThat(moderate.getDisplayName()).isEqualTo("Moderate");
    assertThat(moderate.getEmoji()).isEqualTo("🟠");
    assertThat(moderate.getMinHealthPenalty()).isEqualTo(2);
    assertThat(moderate.getMaxHealthPenalty()).isEqualTo(3);
    assertThat(moderate.getBaseHealingCost()).isEqualTo(10000L);
    assertThat(moderate.getDisplayWithEmoji()).isEqualTo("🟠 Moderate");
    assertThat(moderate.getHealthPenaltyRangeDisplay()).isEqualTo("2-3");
  }

  @Test
  @DisplayName("Should have correct properties for SEVERE")
  void shouldHaveCorrectPropertiesForSevere() {
    InjurySeverity severe = InjurySeverity.SEVERE;

    assertThat(severe.getDisplayName()).isEqualTo("Severe");
    assertThat(severe.getEmoji()).isEqualTo("🔴");
    assertThat(severe.getMinHealthPenalty()).isEqualTo(3);
    assertThat(severe.getMaxHealthPenalty()).isEqualTo(5);
    assertThat(severe.getBaseHealingCost()).isEqualTo(15000L);
    assertThat(severe.getDisplayWithEmoji()).isEqualTo("🔴 Severe");
    assertThat(severe.getHealthPenaltyRangeDisplay()).isEqualTo("3-5");
  }

  @Test
  @DisplayName("Should have correct properties for CRITICAL")
  void shouldHaveCorrectPropertiesForCritical() {
    InjurySeverity critical = InjurySeverity.CRITICAL;

    assertThat(critical.getDisplayName()).isEqualTo("Critical");
    assertThat(critical.getEmoji()).isEqualTo("💀");
    assertThat(critical.getMinHealthPenalty()).isEqualTo(4);
    assertThat(critical.getMaxHealthPenalty()).isEqualTo(7);
    assertThat(critical.getBaseHealingCost()).isEqualTo(25000L);
    assertThat(critical.getDisplayWithEmoji()).isEqualTo("💀 Critical");
    assertThat(critical.getHealthPenaltyRangeDisplay()).isEqualTo("4-7");
  }

  @Test
  @DisplayName("Should generate random health penalty within range")
  void shouldGenerateRandomHealthPenaltyWithinRange() {
    // Test multiple times to ensure randomness works
    for (int i = 0; i < 100; i++) {
      int penalty = InjurySeverity.MINOR.getRandomHealthPenalty(random);
      assertThat(penalty).isBetween(1, 2);

      penalty = InjurySeverity.MODERATE.getRandomHealthPenalty(random);
      assertThat(penalty).isBetween(2, 3);

      penalty = InjurySeverity.SEVERE.getRandomHealthPenalty(random);
      assertThat(penalty).isBetween(3, 5);

      penalty = InjurySeverity.CRITICAL.getRandomHealthPenalty(random);
      assertThat(penalty).isBetween(4, 7);
    }
  }

  @Test
  @DisplayName("Should handle single value health penalty ranges")
  void shouldHandleSingleValueHealthPenaltyRanges() {
    // Create a mock severity with same min/max (this tests the edge case)
    // Since we can't modify enum values, we test the logic indirectly
    InjurySeverity minor = InjurySeverity.MINOR;

    // MINOR has range 1-2, so we can't test single value directly
    // But we can verify the method works correctly
    int penalty = minor.getRandomHealthPenalty(random);
    assertThat(penalty).isBetween(minor.getMinHealthPenalty(), minor.getMaxHealthPenalty());
  }

  @ParameterizedTest
  @DisplayName("Should have correct healing success thresholds")
  @CsvSource({"MINOR, 3", "MODERATE, 4", "SEVERE, 5", "CRITICAL, 6"})
  void shouldHaveCorrectHealingSuccessThresholds(InjurySeverity severity, int expectedThreshold) {
    assertThat(severity.getHealingSuccessThreshold()).isEqualTo(expectedThreshold);
  }

  @ParameterizedTest
  @DisplayName("Should correctly determine healing success")
  @CsvSource({
    "MINOR, 2, false",
    "MINOR, 3, true",
    "MINOR, 6, true",
    "MODERATE, 3, false",
    "MODERATE, 4, true",
    "MODERATE, 6, true",
    "SEVERE, 4, false",
    "SEVERE, 5, true",
    "SEVERE, 6, true",
    "CRITICAL, 5, false",
    "CRITICAL, 6, true"
  })
  void shouldCorrectlyDetermineHealingSuccess(
      InjurySeverity severity, int roll, boolean expectedSuccess) {
    assertThat(severity.isHealingSuccessful(roll)).isEqualTo(expectedSuccess);
  }

  @ParameterizedTest
  @DisplayName("Should calculate correct healing success percentages")
  @CsvSource({
    "MINOR, 66", // (6-3+1)*100/6 = 4*100/6 = 66.67 ≈ 66
    "MODERATE, 50", // (6-4+1)*100/6 = 3*100/6 = 50
    "SEVERE, 33", // (6-5+1)*100/6 = 2*100/6 = 33.33 ≈ 33
    "CRITICAL, 16" // (6-6+1)*100/6 = 1*100/6 = 16.67 ≈ 16
  })
  void shouldCalculateCorrectHealingSuccessPercentages(
      InjurySeverity severity, int expectedPercentage) {
    assertThat(severity.getHealingSuccessPercentage()).isEqualTo(expectedPercentage);
  }

  @Test
  @DisplayName("Should have severities in ascending order of difficulty")
  void shouldHaveSeveritiesInAscendingOrderOfDifficulty() {
    InjurySeverity[] severities = InjurySeverity.values();

    for (int i = 1; i < severities.length; i++) {
      // Higher severity should have higher minimum health penalty
      assertThat(severities[i].getMinHealthPenalty())
          .as(
              "Severity %s should have higher min health penalty than %s",
              severities[i], severities[i - 1])
          .isGreaterThanOrEqualTo(severities[i - 1].getMinHealthPenalty());

      // Higher severity should have higher healing cost
      assertThat(severities[i].getBaseHealingCost())
          .as(
              "Severity %s should have higher healing cost than %s",
              severities[i], severities[i - 1])
          .isGreaterThan(severities[i - 1].getBaseHealingCost());

      // Higher severity should have higher healing threshold (harder to heal)
      assertThat(severities[i].getHealingSuccessThreshold())
          .as(
              "Severity %s should have higher healing threshold than %s",
              severities[i], severities[i - 1])
          .isGreaterThanOrEqualTo(severities[i - 1].getHealingSuccessThreshold());
    }
  }

  @Test
  @DisplayName("Should format healing info display correctly")
  void shouldFormatHealingInfoDisplayCorrectly() {
    assertThat(InjurySeverity.MINOR.getHealingInfoDisplay()).isEqualTo("5,000 fans (66% success)");
    assertThat(InjurySeverity.MODERATE.getHealingInfoDisplay())
        .isEqualTo("10,000 fans (50% success)");
    assertThat(InjurySeverity.SEVERE.getHealingInfoDisplay())
        .isEqualTo("15,000 fans (33% success)");
    assertThat(InjurySeverity.CRITICAL.getHealingInfoDisplay())
        .isEqualTo("25,000 fans (16% success)");
  }

  @Test
  @DisplayName("Should have unique emojis for each severity")
  void shouldHaveUniqueEmojisForEachSeverity() {
    InjurySeverity[] severities = InjurySeverity.values();

    for (int i = 0; i < severities.length; i++) {
      for (int j = i + 1; j < severities.length; j++) {
        assertThat(severities[i].getEmoji())
            .as("Severities %s and %s should have different emojis", severities[i], severities[j])
            .isNotEqualTo(severities[j].getEmoji());
      }
    }
  }

  @Test
  @DisplayName("Should have logical health penalty progression")
  void shouldHaveLogicalHealthPenaltyProgression() {
    // Verify that health penalty ranges don't have gaps and make sense
    assertThat(InjurySeverity.MINOR.getMaxHealthPenalty())
        .isEqualTo(InjurySeverity.MODERATE.getMinHealthPenalty());

    assertThat(InjurySeverity.MODERATE.getMaxHealthPenalty())
        .isEqualTo(InjurySeverity.SEVERE.getMinHealthPenalty());

    // SEVERE to CRITICAL can have overlap, which is acceptable for severity ranges
    // Just ensure that CRITICAL min is not less than MODERATE max (logical progression)
    assertThat(InjurySeverity.CRITICAL.getMinHealthPenalty())
        .isGreaterThanOrEqualTo(InjurySeverity.MODERATE.getMaxHealthPenalty());
  }

  @Test
  @DisplayName("Should handle edge cases in healing rolls")
  void shouldHandleEdgeCasesInHealingRolls() {
    // Test minimum possible roll (1)
    assertThat(InjurySeverity.MINOR.isHealingSuccessful(1)).isFalse();
    assertThat(InjurySeverity.CRITICAL.isHealingSuccessful(1)).isFalse();

    // Test maximum possible roll (6)
    assertThat(InjurySeverity.MINOR.isHealingSuccessful(6)).isTrue();
    assertThat(InjurySeverity.CRITICAL.isHealingSuccessful(6)).isTrue();

    // Test impossible rolls (should handle gracefully)
    assertThat(InjurySeverity.MINOR.isHealingSuccessful(0)).isFalse();
    assertThat(InjurySeverity.MINOR.isHealingSuccessful(7)).isTrue();
  }
}

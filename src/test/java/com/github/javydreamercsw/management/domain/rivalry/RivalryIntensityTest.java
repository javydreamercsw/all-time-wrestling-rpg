package com.github.javydreamercsw.management.domain.rivalry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for RivalryIntensity enum. Tests the ATW RPG rivalry intensity system functionality.
 */
@DisplayName("RivalryIntensity Tests")
class RivalryIntensityTest {

  @Test
  @DisplayName("Should have correct intensity count")
  void shouldHaveCorrectIntensityCount() {
    assertThat(RivalryIntensity.values()).hasSize(4);
  }

  @ParameterizedTest
  @DisplayName("Should determine correct intensity from heat")
  @CsvSource({
    "0, SIMMERING",
    "5, SIMMERING",
    "9, SIMMERING",
    "10, HEATED",
    "15, HEATED",
    "19, HEATED",
    "20, INTENSE",
    "25, INTENSE",
    "29, INTENSE",
    "30, EXPLOSIVE",
    "35, EXPLOSIVE",
    "100, EXPLOSIVE"
  })
  void shouldDetermineCorrectIntensityFromHeat(int heat, RivalryIntensity expectedIntensity) {
    assertThat(RivalryIntensity.fromHeat(heat)).isEqualTo(expectedIntensity);
  }

  @Test
  @DisplayName("Should have correct properties for SIMMERING")
  void shouldHaveCorrectPropertiesForSimmering() {
    RivalryIntensity simmering = RivalryIntensity.SIMMERING;

    assertThat(simmering.getDisplayName()).isEqualTo("Simmering");
    assertThat(simmering.getEmoji()).isEqualTo("😐");
    assertThat(simmering.getMinHeat()).isEqualTo(0);
    assertThat(simmering.getMaxHeat()).isEqualTo(9);
    assertThat(simmering.getDescription()).contains("Early stages");
    assertThat(simmering.getDisplayWithEmoji()).isEqualTo("😐 Simmering");
    assertThat(simmering.getHeatRangeDisplay()).isEqualTo("0-9 heat");
  }

  @Test
  @DisplayName("Should have correct properties for HEATED")
  void shouldHaveCorrectPropertiesForHeated() {
    RivalryIntensity heated = RivalryIntensity.HEATED;

    assertThat(heated.getDisplayName()).isEqualTo("Heated");
    assertThat(heated.getEmoji()).isEqualTo("🔥");
    assertThat(heated.getMinHeat()).isEqualTo(10);
    assertThat(heated.getMaxHeat()).isEqualTo(19);
    assertThat(heated.getDescription()).contains("Must wrestle at next show");
    assertThat(heated.getDisplayWithEmoji()).isEqualTo("🔥 Heated");
    assertThat(heated.getHeatRangeDisplay()).isEqualTo("10-19 heat");
  }

  @Test
  @DisplayName("Should have correct properties for INTENSE")
  void shouldHaveCorrectPropertiesForIntense() {
    RivalryIntensity intense = RivalryIntensity.INTENSE;

    assertThat(intense.getDisplayName()).isEqualTo("Intense");
    assertThat(intense.getEmoji()).isEqualTo("💥");
    assertThat(intense.getMinHeat()).isEqualTo(20);
    assertThat(intense.getMaxHeat()).isEqualTo(29);
    assertThat(intense.getDescription()).contains("Can attempt resolution");
    assertThat(intense.getDisplayWithEmoji()).isEqualTo("💥 Intense");
    assertThat(intense.getHeatRangeDisplay()).isEqualTo("20-29 heat");
  }

  @Test
  @DisplayName("Should have correct properties for EXPLOSIVE")
  void shouldHaveCorrectPropertiesForExplosive() {
    RivalryIntensity explosive = RivalryIntensity.EXPLOSIVE;

    assertThat(explosive.getDisplayName()).isEqualTo("Explosive");
    assertThat(explosive.getEmoji()).isEqualTo("🌋");
    assertThat(explosive.getMinHeat()).isEqualTo(30);
    assertThat(explosive.getMaxHeat()).isEqualTo(Integer.MAX_VALUE);
    assertThat(explosive.getDescription()).contains("Requires stipulation match");
    assertThat(explosive.getDisplayWithEmoji()).isEqualTo("🌋 Explosive");
    assertThat(explosive.getHeatRangeDisplay()).isEqualTo("30+ heat");
  }

  @ParameterizedTest
  @DisplayName("Should correctly check if requires next show match")
  @CsvSource({"SIMMERING, false", "HEATED, true", "INTENSE, true", "EXPLOSIVE, true"})
  void shouldCorrectlyCheckIfRequiresNextShowMatch(RivalryIntensity intensity, boolean expected) {
    assertThat(intensity.requiresNextShowMatch()).isEqualTo(expected);
  }

  @ParameterizedTest
  @DisplayName("Should correctly check if allows resolution attempt")
  @CsvSource({"SIMMERING, false", "HEATED, false", "INTENSE, true", "EXPLOSIVE, true"})
  void shouldCorrectlyCheckIfAllowsResolutionAttempt(RivalryIntensity intensity, boolean expected) {
    assertThat(intensity.allowsResolutionAttempt()).isEqualTo(expected);
  }

  @ParameterizedTest
  @DisplayName("Should correctly check if requires stipulation match")
  @CsvSource({"SIMMERING, false", "HEATED, false", "INTENSE, false", "EXPLOSIVE, true"})
  void shouldCorrectlyCheckIfRequiresStipulationMatch(
      RivalryIntensity intensity, boolean expected) {
    assertThat(intensity.requiresStipulationMatch()).isEqualTo(expected);
  }

  @Test
  @DisplayName("Should have intensities in ascending order")
  void shouldHaveIntensitiesInAscendingOrder() {
    RivalryIntensity[] intensities = RivalryIntensity.values();

    for (int i = 1; i < intensities.length; i++) {
      assertThat(intensities[i].getMinHeat())
          .as(
              "Intensity %s should have higher minimum heat than %s",
              intensities[i], intensities[i - 1])
          .isGreaterThan(intensities[i - 1].getMinHeat());
    }
  }

  @Test
  @DisplayName("Should have no gaps in heat ranges")
  void shouldHaveNoGapsInHeatRanges() {
    RivalryIntensity[] intensities = RivalryIntensity.values();

    for (int i = 1; i < intensities.length; i++) {
      if (intensities[i - 1].getMaxHeat() != Integer.MAX_VALUE) {
        assertThat(intensities[i].getMinHeat())
            .as("No gap should exist between %s and %s", intensities[i - 1], intensities[i])
            .isEqualTo(intensities[i - 1].getMaxHeat() + 1);
      }
    }
  }

  @Test
  @DisplayName("Should handle edge case heat values")
  void shouldHandleEdgeCaseHeatValues() {
    // Test exact boundaries
    assertThat(RivalryIntensity.fromHeat(9)).isEqualTo(RivalryIntensity.SIMMERING);
    assertThat(RivalryIntensity.fromHeat(10)).isEqualTo(RivalryIntensity.HEATED);
    assertThat(RivalryIntensity.fromHeat(19)).isEqualTo(RivalryIntensity.HEATED);
    assertThat(RivalryIntensity.fromHeat(20)).isEqualTo(RivalryIntensity.INTENSE);
    assertThat(RivalryIntensity.fromHeat(29)).isEqualTo(RivalryIntensity.INTENSE);
    assertThat(RivalryIntensity.fromHeat(30)).isEqualTo(RivalryIntensity.EXPLOSIVE);
  }

  @Test
  @DisplayName("Should handle very high heat values")
  void shouldHandleVeryHighHeatValues() {
    assertThat(RivalryIntensity.fromHeat(1000)).isEqualTo(RivalryIntensity.EXPLOSIVE);
    assertThat(RivalryIntensity.fromHeat(Integer.MAX_VALUE)).isEqualTo(RivalryIntensity.EXPLOSIVE);
  }

  @Test
  @DisplayName("Should handle negative heat values")
  void shouldHandleNegativeHeatValues() {
    // Negative heat should default to SIMMERING (lowest intensity)
    assertThat(RivalryIntensity.fromHeat(-1))
        .isEqualTo(RivalryIntensity.EXPLOSIVE); // Falls through to fallback
  }

  @Test
  @DisplayName("Should have consistent ordinal progression")
  void shouldHaveConsistentOrdinalProgression() {
    assertThat(RivalryIntensity.SIMMERING.ordinal()).isEqualTo(0);
    assertThat(RivalryIntensity.HEATED.ordinal()).isEqualTo(1);
    assertThat(RivalryIntensity.INTENSE.ordinal()).isEqualTo(2);
    assertThat(RivalryIntensity.EXPLOSIVE.ordinal()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should have unique emojis for each intensity")
  void shouldHaveUniqueEmojisForEachIntensity() {
    RivalryIntensity[] intensities = RivalryIntensity.values();

    for (int i = 0; i < intensities.length; i++) {
      for (int j = i + 1; j < intensities.length; j++) {
        assertThat(intensities[i].getEmoji())
            .as(
                "Intensities %s and %s should have different emojis",
                intensities[i], intensities[j])
            .isNotEqualTo(intensities[j].getEmoji());
      }
    }
  }
}

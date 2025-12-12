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
package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for WrestlerTier enum. Tests the ATW RPG tier system functionality. */
@DisplayName("WrestlerTier Tests")
class WrestlerTierTest {

  @Test
  @DisplayName("Should have correct tier count")
  void shouldHaveCorrectTierCount() {
    assertThat(WrestlerTier.values()).hasSize(6);
  }

  @ParameterizedTest
  @DisplayName("Should determine correct tier from fan count")
  @CsvSource({
    "0, ROOKIE",
    "10000, ROOKIE",
    "24999, ROOKIE",
    "25000, RISER",
    "30000, RISER",
    "39999, RISER",
    "40000, CONTENDER",
    "50000, CONTENDER",
    "59999, CONTENDER",
    "60000, MIDCARDER",
    "80000, MIDCARDER",
    "99999, MIDCARDER",
    "100000, MAIN_EVENTER",
    "120000, MAIN_EVENTER",
    "149999, MAIN_EVENTER",
    "150000, ICON",
    "200000, ICON",
    "999999, ICON"
  })
  void shouldDetermineCorrectTierFromFanCount(Long fans, WrestlerTier expectedTier) {
    assertThat(WrestlerTier.fromFanCount(fans)).isEqualTo(expectedTier);
  }

  @Test
  @DisplayName("Should handle null fan count")
  void shouldHandleNullFanCount() {
    assertThat(WrestlerTier.fromFanCount(null)).isEqualTo(WrestlerTier.ROOKIE);
  }

  @Test
  @DisplayName("Should handle negative fan count")
  void shouldHandleNegativeFanCount() {
    assertThat(WrestlerTier.fromFanCount(-1000L)).isEqualTo(WrestlerTier.ROOKIE);
  }

  @Test
  @DisplayName("Should have correct display properties for ROOKIE")
  void shouldHaveCorrectDisplayPropertiesForRookie() {
    WrestlerTier rookie = WrestlerTier.ROOKIE;

    assertThat(rookie.getDisplayName()).isEqualTo("Rookie");
    assertThat(rookie.getEmoji()).isEqualTo("🐍");
    assertThat(rookie.getMinFans()).isEqualTo(0L);
    assertThat(rookie.getMaxFans()).isEqualTo(24999L);
    assertThat(rookie.getDescription()).contains("Just starting out");
    assertThat(rookie.getDisplayWithEmoji()).isEqualTo("🐍 Rookie");
    assertThat(rookie.getFanRangeDisplay()).isEqualTo("0 - 24,999");
  }

  @Test
  @DisplayName("Should have correct display properties for ICON")
  void shouldHaveCorrectDisplayPropertiesForIcon() {
    WrestlerTier icon = WrestlerTier.ICON;

    assertThat(icon.getDisplayName()).isEqualTo("Icon");
    assertThat(icon.getEmoji()).isEqualTo("🌟");
    assertThat(icon.getMinFans()).isEqualTo(150000L);
    assertThat(icon.getMaxFans()).isEqualTo(Long.MAX_VALUE);
    assertThat(icon.getDescription()).contains("Transcends eras");
    assertThat(icon.getDisplayWithEmoji()).isEqualTo("🌟 Icon");
    assertThat(icon.getFanRangeDisplay()).isEqualTo("150,000+");
  }

  @Test
  @DisplayName("Should have no gaps in fan ranges")
  void shouldHaveNoGapsInFanRanges() {
    WrestlerTier[] tiers = WrestlerTier.values();

    for (int i = 1; i < tiers.length; i++) {
      if (tiers[i - 1].getMaxFans() != Long.MAX_VALUE) {
        assertThat(tiers[i].getMinFans())
            .as("No gap should exist between %s and %s", tiers[i - 1], tiers[i])
            .isEqualTo(tiers[i - 1].getMaxFans() + 1);
      }
    }
  }
}

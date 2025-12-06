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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for TitleTier enum. Tests the ATW RPG championship system functionality. */
@DisplayName("TitleTier Tests")
class TitleTierTest {

  @Test
  @DisplayName("Should have correct title count")
  void shouldHaveCorrectTitleCount() {
    assertThat(TitleTier.values()).hasSize(4);
  }

  @Test
  @DisplayName("Should have correct properties for EXTREME title")
  void shouldHaveCorrectPropertiesForExtremeTitle() {
    TitleTier extreme = TitleTier.EXTREME;

    assertThat(extreme.getTitleName()).isEqualTo("Extreme Title");
    assertThat(extreme.getRequiredFans()).isEqualTo(25000L);
    assertThat(extreme.getChallengeCost()).isEqualTo(15000L);
    assertThat(extreme.getContenderEntryFee()).isEqualTo(15000L);
    assertThat(extreme.getDescription()).contains("First step up");
    assertThat(extreme.getDisplayWithRequirement())
        .isEqualTo("Extreme Title (25,000 fans required)");
    assertThat(extreme.getPrestigeRank()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should have correct properties for WORLD title")
  void shouldHaveCorrectPropertiesForWorldTitle() {
    TitleTier world = TitleTier.WORLD;

    assertThat(world.getTitleName()).isEqualTo("World Title");
    assertThat(world.getRequiredFans()).isEqualTo(100000L);
    assertThat(world.getChallengeCost()).isEqualTo(15000L);
    assertThat(world.getContenderEntryFee()).isEqualTo(15000L);
    assertThat(world.getDescription()).contains("Main event");
    assertThat(world.getDisplayWithRequirement()).isEqualTo("World Title (100,000 fans required)");
    assertThat(world.getPrestigeRank()).isEqualTo(3);
  }

  @ParameterizedTest
  @DisplayName("Should correctly check wrestler eligibility")
  @CsvSource({
    "0, false, false, false, false",
    "20000, false, false, false, false",
    "25000, true, false, false, false",
    "40000, true, true, false, false",
    "60000, true, true, true, false",
    "100000, true, true, true, true",
    "150000, true, true, true, true"
  })
  void shouldCorrectlyCheckWrestlerEligibility(
      Long fans, boolean extreme, boolean tagTeam, boolean intertemporal, boolean world) {
    assertThat(TitleTier.EXTREME.isEligible(fans)).isEqualTo(extreme);
    assertThat(TitleTier.TAG_TEAM.isEligible(fans)).isEqualTo(tagTeam);
    assertThat(TitleTier.INTERTEMPORAL.isEligible(fans)).isEqualTo(intertemporal);
    assertThat(TitleTier.WORLD.isEligible(fans)).isEqualTo(world);
  }

  @Test
  @DisplayName("Should handle null fan count in eligibility check")
  void shouldHandleNullFanCountInEligibilityCheck() {
    assertThat(TitleTier.EXTREME.isEligible(null)).isFalse();
    assertThat(TitleTier.WORLD.isEligible(null)).isFalse();
  }

  @Test
  @DisplayName("Should get eligible titles for wrestler with 50k fans")
  void shouldGetEligibleTitlesForWrestlerWith50kFans() {
    TitleTier[] eligible = TitleTier.getEligibleTitles(50000L);

    assertThat(eligible).hasSize(2);
    assertThat(eligible).containsExactly(TitleTier.EXTREME, TitleTier.TAG_TEAM);
  }

  @Test
  @DisplayName("Should get eligible titles for wrestler with 150k fans")
  void shouldGetEligibleTitlesForWrestlerWith150kFans() {
    TitleTier[] eligible = TitleTier.getEligibleTitles(150000L);

    assertThat(eligible).hasSize(4);
    assertThat(eligible)
        .containsExactly(
            TitleTier.EXTREME, TitleTier.TAG_TEAM, TitleTier.INTERTEMPORAL, TitleTier.WORLD);
  }

  @Test
  @DisplayName("Should get no eligible titles for rookie wrestler")
  void shouldGetNoEligibleTitlesForRookieWrestler() {
    TitleTier[] eligible = TitleTier.getEligibleTitles(10000L);

    assertThat(eligible).isEmpty();
  }

  @Test
  @DisplayName("Should handle null fan count in getEligibleTitles")
  void shouldHandleNullFanCountInGetEligibleTitles() {
    TitleTier[] eligible = TitleTier.getEligibleTitles(null);

    assertThat(eligible).isEmpty();
  }

  @Test
  @DisplayName("Should get highest eligible title for main eventer")
  void shouldGetHighestEligibleTitleForMainEventer() {
    TitleTier highest = TitleTier.getHighestEligibleTitle(120000L);

    assertThat(highest).isEqualTo(TitleTier.WORLD);
  }

  @Test
  @DisplayName("Should get highest eligible title for contender")
  void shouldGetHighestEligibleTitleForContender() {
    TitleTier highest = TitleTier.getHighestEligibleTitle(45000L);

    assertThat(highest).isEqualTo(TitleTier.TAG_TEAM);
  }

  @Test
  @DisplayName("Should return null for wrestler with no eligible titles")
  void shouldReturnNullForWrestlerWithNoEligibleTitles() {
    TitleTier highest = TitleTier.getHighestEligibleTitle(5000L);

    assertThat(highest).isNull();
  }

  @Test
  @DisplayName("Should have titles in ascending order of prestige")
  void shouldHaveTitlesInAscendingOrderOfPrestige() {
    TitleTier[] titles = TitleTier.values();

    for (int i = 1; i < titles.length; i++) {
      assertThat(titles[i].getRequiredFans())
          .as("Title %s should require more fans than %s", titles[i], titles[i - 1])
          .isGreaterThan(titles[i - 1].getRequiredFans());

      assertThat(titles[i].getPrestigeRank())
          .as("Title %s should have higher prestige rank than %s", titles[i], titles[i - 1])
          .isGreaterThan(titles[i - 1].getPrestigeRank());
    }
  }

  @Test
  @DisplayName("Should have consistent challenge costs")
  void shouldHaveConsistentChallengeCosts() {
    for (TitleTier title : TitleTier.values()) {
      assertThat(title.getChallengeCost())
          .as("All titles should have the same challenge cost")
          .isEqualTo(15000L);

      assertThat(title.getContenderEntryFee())
          .as("Contender entry fee should match challenge cost")
          .isEqualTo(title.getChallengeCost());
    }
  }
}

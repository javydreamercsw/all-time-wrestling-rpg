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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for TitleService. */
@DisplayName("TitleService Integration Tests")
@Transactional
class TitleServiceTest extends ManagementIntegrationTest {

  @Autowired private TitleService titleService;

  @Test
  @DisplayName("Should create and award title")
  void shouldCreateAndAwardTitle() {
    // Given
    Wrestler champion = createTestWrestler("Champion");

    // When
    Title title =
        titleService.createTitle(
            "World Championship",
            "The top prize",
            WrestlerTier.MAIN_EVENTER,
            ChampionshipType.SINGLE,
            defaultUniverse.getId());

    titleService.awardTitleTo(title, List.of(champion));

    // Then
    Title updatedTitle = titleRepository.findById(title.getId()).orElseThrow();
    assertThat(updatedTitle.getCurrentChampions()).contains(champion);
    assertThat(updatedTitle.getCurrentReign()).isPresent();
    assertThat(updatedTitle.getCurrentReign().get().getChampions()).contains(champion);
  }

  @Test
  @DisplayName("Should vacate title")
  void shouldVacateTitle() {
    // Given
    Wrestler champion = createTestWrestler("Champion");
    Title title =
        titleService.createTitle(
            "World Championship",
            "The top prize",
            WrestlerTier.MAIN_EVENTER,
            ChampionshipType.SINGLE,
            defaultUniverse.getId());
    titleService.awardTitleTo(title, List.of(champion));

    // When
    titleService.vacateTitle(title.getId());

    // Then
    Title updatedTitle = titleRepository.findById(title.getId()).orElseThrow();
    assertThat(updatedTitle.getCurrentChampions()).isEmpty();
    assertThat(updatedTitle.getCurrentReign()).isEmpty();
  }

  @Test
  @DisplayName("Should find eligible titles for fan count")
  void shouldFindEligibleTitlesForFanCount() {
    // Given
    titleService.createTitle(
        "World Championship",
        "The top prize",
        WrestlerTier.MAIN_EVENTER,
        ChampionshipType.SINGLE,
        defaultUniverse.getId());
    titleService.createTitle(
        "Midcard Championship",
        "The midcard prize",
        WrestlerTier.MIDCARDER,
        ChampionshipType.SINGLE,
        defaultUniverse.getId());

    // When
    List<Title> worldEligible = titleService.findEligibleTitlesForFanCount(100_000L);
    List<Title> midcardEligible = titleService.findEligibleTitlesForFanCount(60_000L);

    // Then
    assertThat(worldEligible).extracting(Title::getName).contains("World Championship");
    assertThat(midcardEligible).extracting(Title::getName).contains("Midcard Championship");
    assertThat(worldEligible).extracting(Title::getName).doesNotContain("Midcard Championship");
    assertThat(midcardEligible).extracting(Title::getName).doesNotContain("World Championship");
  }
}

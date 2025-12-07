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
package com.github.javydreamercsw.management.domain.title;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Unit tests for Title entity. Tests the ATW RPG championship system functionality. */
@DisplayName("Title Tests")
@DataJpaTest
class TitleTest {
  @Autowired private WrestlerRepository wrestlerRepository;

  private Title title;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    title = new Title();
    title.setName("Test Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setDescription("Test title for unit tests");

    wrestler1 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 1"));
    wrestler2 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 2"));
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Title newTitle = new Title();

    assertThat(newTitle.getIsActive()).isTrue();
    assertThat(newTitle.isVacant()).isTrue();
    assertThat(newTitle.getCurrentChampions()).isEmpty();
    assertThat(newTitle.getTitleReigns()).isEmpty();
  }

  @Test
  @DisplayName("Should award title to new champion")
  void shouldAwardTitleToNewChampion() {
    assertThat(title.isVacant()).isTrue();
    assertThat(title.getCurrentChampions()).isEmpty();

    Instant awardTime = Instant.now();
    title.awardTitleTo(List.of(wrestler1), awardTime);

    // Title should now be held
    assertThat(title.isVacant()).isFalse();
    assertThat(title.getCurrentChampions()).containsExactly(wrestler1);

    // Should create title reign
    assertThat(title.getTitleReigns()).hasSize(1);
    TitleReign reign = title.getTitleReigns().iterator().next();
    assertThat(reign.getChampions()).containsExactly(wrestler1);
    assertThat(reign.getTitle()).isEqualTo(title);
    assertThat(reign.isCurrentReign()).isTrue();
    assertThat(reign.getStartDate()).isEqualTo(awardTime);
  }

  @Test
  @DisplayName("Should transfer title between champions")
  void shouldTransferTitleBetweenChampions() {
    // Award to first champion
    Instant firstAwardTime = Instant.now();
    title.awardTitleTo(List.of(wrestler1), firstAwardTime);
    TitleReign firstReign = title.getCurrentReign().orElseThrow();

    // Award to second champion
    Instant secondAwardTime = Instant.now().plusSeconds(60);
    title.awardTitleTo(List.of(wrestler2), secondAwardTime);

    // First reign should be ended
    assertThat(firstReign.isCurrentReign()).isFalse();
    assertThat(firstReign.getEndDate()).isEqualTo(secondAwardTime);

    // Second reign should be current
    assertThat(title.getCurrentChampions()).containsExactly(wrestler2);
    assertThat(title.getTitleReigns()).hasSize(2);

    TitleReign currentReign = title.getCurrentReign().orElseThrow();
    assertThat(currentReign.getChampions()).containsExactly(wrestler2);
    assertThat(currentReign.isCurrentReign()).isTrue();
  }

  @Test
  @DisplayName("Should vacate title properly")
  void shouldVacateTitleProperly() {
    // Award title first
    title.awardTitleTo(List.of(wrestler1), Instant.now());
    TitleReign reign = title.getCurrentReign().orElseThrow();

    // Vacate title
    title.vacateTitle();

    // Title should be vacant
    assertThat(title.isVacant()).isTrue();
    assertThat(title.getCurrentChampions()).isEmpty();

    // Reign should be ended
    assertThat(reign.isCurrentReign()).isFalse();
    assertThat(reign.getEndDate()).isNotNull();
  }

  @Test
  @DisplayName("Should calculate current reign days")
  void shouldCalculateCurrentReignDays() {
    // Vacant title
    assertThat(title.getCurrentReignDays()).isEqualTo(0);

    // Award title
    Instant awardTime = Instant.now().minus(1, ChronoUnit.DAYS);
    title.awardTitleTo(List.of(wrestler1), awardTime);

    // Should be 1 day
    assertThat(title.getCurrentReignDays()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should count total reigns")
  void shouldCountTotalReigns() {
    assertThat(title.getTotalReigns()).isEqualTo(0);

    title.awardTitleTo(List.of(wrestler1), Instant.now());
    assertThat(title.getTotalReigns()).isEqualTo(1);

    title.awardTitleTo(List.of(wrestler2), Instant.now());
    assertThat(title.getTotalReigns()).isEqualTo(2);

    title.vacateTitle();
    assertThat(title.getTotalReigns()).isEqualTo(2); // Vacating doesn't add a new reign
  }

  @Test
  @DisplayName("Should check wrestler eligibility")
  void shouldCheckWrestlerEligibility() {
    title.setTier(WrestlerTier.MAIN_EVENTER); // Requires 100k fans

    Wrestler eligibleWrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Eligible"));
    eligibleWrestler.setFans(120000L);
    Wrestler ineligibleWrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Ineligible"));
    ineligibleWrestler.setFans(50000L);

    assertThat(title.isWrestlerEligible(eligibleWrestler)).isTrue();
    assertThat(title.isWrestlerEligible(ineligibleWrestler)).isFalse();
  }

  @Test
  @DisplayName("Should get challenge costs from tier")
  void shouldGetChallengeCostsFromTier() {
    title.setTier(WrestlerTier.CONTENDER);
    assertThat(title.getChallengeCost()).isEqualTo(40000L);
    assertThat(title.getContenderEntryFee()).isEqualTo(15000L);

    title.setTier(WrestlerTier.MAIN_EVENTER);
    assertThat(title.getChallengeCost()).isEqualTo(100000L);
    assertThat(title.getContenderEntryFee()).isEqualTo(15000L);
  }

  @Test
  @DisplayName("Should create display names")
  void shouldCreateDisplayNames() {
    // Vacant title
    title.setName("World Championship");
    assertThat(title.getDisplayName()).isEqualTo("World Championship (Vacant)");

    // Title with champion
    title.awardTitleTo(List.of(wrestler1), Instant.now());
    assertThat(title.getDisplayName()).isEqualTo("World Championship (Champion: Wrestler 1)");
  }

  @Test
  @DisplayName("Should show status emojis")
  void shouldShowStatusEmojis() {
    // Active vacant title
    title.setIsActive(true);
    assertThat(title.getStatusEmoji()).isEqualTo("👑❓");

    // Active title with champion
    title.awardTitleTo(List.of(wrestler1), Instant.now());
    assertThat(title.getStatusEmoji()).isEqualTo("👑");

    // Inactive title
    title.setIsActive(false);
    assertThat(title.getStatusEmoji()).isEqualTo("🚫");
  }

  @Test
  @DisplayName("Should handle multiple title changes")
  void shouldHandleMultipleTitleChanges() {
    // Award to wrestler1
    title.awardTitleTo(List.of(wrestler1), Instant.now());
    assertThat(title.getTotalReigns()).isEqualTo(1);

    // Award to wrestler2
    title.awardTitleTo(List.of(wrestler2), Instant.now());
    assertThat(title.getTotalReigns()).isEqualTo(2);

    // Award back to wrestler1 (second reign)
    title.awardTitleTo(List.of(wrestler1), Instant.now());
    assertThat(title.getTotalReigns()).isEqualTo(3);

    // Verify current champion
    assertThat(title.getCurrentChampions()).containsExactly(wrestler1);

    // Verify only one current reign
    long currentReigns = title.getTitleReigns().stream().filter(TitleReign::isCurrentReign).count();
    assertThat(currentReigns).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle vacating already vacant title")
  void shouldHandleVacatingAlreadyVacantTitle() {
    // Title is vacant by default
    assertThat(title.isVacant()).isTrue();

    // Vacating vacant title should not cause issues
    title.vacateTitle();

    assertThat(title.isVacant()).isTrue();
    assertThat(title.getCurrentChampions()).isEmpty();
    assertThat(title.getTitleReigns()).isEmpty();
  }

  @Test
  @DisplayName("Should return champion from champion field directly")
  void shouldReturnChampionFromChampionFieldDirectly() {
    // Set champion directly, without creating a reign
    title.setChampion(List.of(wrestler1));

    // The getCurrentChampions method should now return the champion
    assertThat(title.getCurrentChampions()).containsExactly(wrestler1);
  }
}

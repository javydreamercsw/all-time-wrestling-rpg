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
package com.github.javydreamercsw.management.domain.rivalry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Unit tests for Rivalry entity. Tests the ATW RPG rivalry and heat system functionality. */
@DisplayName("Rivalry Tests")
class RivalryTest extends AbstractJpaTest {
  @Autowired private WrestlerRepository wrestlerRepository;

  private Rivalry rivalry;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;

  @Override
  @BeforeEach
  public void baseSetUp() {
    super.baseSetUp();
    wrestler1 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 1"));
    wrestler2 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 2"));
    wrestler3 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 3"));

    rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(0);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now()); // Initialize started date
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Rivalry newRivalry = new Rivalry();

    assertThat(newRivalry.getHeat()).isEqualTo(0);
    assertThat(newRivalry.getIsActive()).isTrue();
    assertThat(newRivalry.getHeatEvents()).isEmpty();
  }

  @Test
  @DisplayName("Should add heat and create heat events")
  void shouldAddHeatAndCreateHeatEvents() {
    assertThat(rivalry.getHeat()).isEqualTo(0);
    assertThat(rivalry.getHeatEvents()).isEmpty();

    rivalry.addHeat(5, "Backstage confrontation");

    assertThat(rivalry.getHeat()).isEqualTo(5);
    assertThat(rivalry.getHeatEvents()).hasSize(1);

    HeatEvent event = rivalry.getHeatEvents().get(0);
    assertThat(event.getHeatChange()).isEqualTo(5);
    assertThat(event.getReason()).isEqualTo("Backstage confrontation");
    assertThat(event.getHeatAfterEvent()).isEqualTo(5);
    assertThat(event.getRivalry()).isEqualTo(rivalry);
  }

  @Test
  @DisplayName("Should accumulate heat over multiple events")
  void shouldAccumulateHeatOverMultipleEvents() {
    rivalry.addHeat(3, "First incident");
    rivalry.addHeat(4, "Second incident");
    rivalry.addHeat(2, "Third incident");

    assertThat(rivalry.getHeat()).isEqualTo(9);
    assertThat(rivalry.getHeatEvents()).hasSize(3);

    // Check heat progression
    assertThat(rivalry.getHeatEvents().get(0).getHeatAfterEvent()).isEqualTo(3);
    assertThat(rivalry.getHeatEvents().get(1).getHeatAfterEvent()).isEqualTo(7);
    assertThat(rivalry.getHeatEvents().get(2).getHeatAfterEvent()).isEqualTo(9);
  }

  @Test
  @DisplayName("Should determine when wrestlers must wrestle next show")
  void shouldDetermineWhenWrestlersMustWrestleNextShow() {
    // Below 25 heat - no requirement
    rivalry.setHeat(24);
    assertThat(rivalry.mustWrestleNextShow()).isFalse();

    // At 25 heat - must wrestle
    rivalry.setHeat(25);
    assertThat(rivalry.mustWrestleNextShow()).isTrue();

    // Above 25 heat - still must wrestle
    rivalry.setHeat(40);
    assertThat(rivalry.mustWrestleNextShow()).isTrue();

    // Inactive rivalry - no requirement
    rivalry.setIsActive(false);
    assertThat(rivalry.mustWrestleNextShow()).isFalse();
  }

  @Test
  @DisplayName("Should determine when rivalry can attempt resolution")
  void shouldDetermineWhenRivalryCanAttemptResolution() {
    // Below 50 heat - cannot attempt
    rivalry.setHeat(49);
    assertThat(rivalry.canAttemptResolution()).isFalse();

    // At 50 heat - can attempt
    rivalry.setHeat(50);
    assertThat(rivalry.canAttemptResolution()).isTrue();

    // Above 50 heat - can attempt
    rivalry.setHeat(65);
    assertThat(rivalry.canAttemptResolution()).isTrue();

    // Inactive rivalry - cannot attempt
    rivalry.setIsActive(false);
    assertThat(rivalry.canAttemptResolution()).isFalse();
  }

  @Test
  @DisplayName("Should determine when rivalry requires rule segment")
  void shouldDetermineWhenRivalryRequiresStipulationMatch() {
    // Below 75 heat - no rule required
    rivalry.setHeat(74);
    assertThat(rivalry.requiresStipulationMatch()).isFalse();

    // At 75 heat - rule required
    rivalry.setHeat(75);
    assertThat(rivalry.requiresStipulationMatch()).isTrue();

    // Above 75 heat - rule required
    rivalry.setHeat(90);
    assertThat(rivalry.requiresStipulationMatch()).isTrue();

    // Inactive rivalry - no rule required
    rivalry.setIsActive(false);
    assertThat(rivalry.requiresStipulationMatch()).isFalse();
  }

  @Test
  @DisplayName("Should attempt resolution with dice rolls")
  void shouldAttemptResolutionWithDiceRolls() {
    rivalry.setHeat(55); // Eligible for resolution
    int initialEventCount = rivalry.getHeatEvents().size();

    // Successful resolution (total > 30)
    boolean resolved = rivalry.attemptResolution(16, 15); // Total = 31
    assertThat(resolved).isTrue();
    assertThat(rivalry.getIsActive()).isFalse();
    assertThat(rivalry.getEndedDate()).isNotNull();

    // Exactly ONE heat event records the resolution — no duplicate end event.
    assertThat(rivalry.getHeatEvents()).hasSize(initialEventCount + 1);

    // Find the resolution event
    HeatEvent resolutionEvent =
        rivalry.getHeatEvents().stream()
            .filter(event -> event.getReason().contains("Resolved by dice roll"))
            .findFirst()
            .orElseThrow();
    assertThat(resolutionEvent.getReason()).contains("Rivalry ended");
    assertThat(resolutionEvent.getReason()).contains("= 31");
  }

  @Test
  @DisplayName("Should fail resolution with low dice rolls")
  void shouldFailResolutionWithLowDiceRolls() {
    rivalry.setHeat(55); // Eligible for resolution

    // Failed resolution (total <= 30)
    boolean resolved = rivalry.attemptResolution(10, 15); // Total = 25
    assertThat(resolved).isFalse();
    assertThat(rivalry.getIsActive()).isTrue();
    assertThat(rivalry.getEndedDate()).isNull();

    // Check heat event was created
    assertThat(rivalry.getHeatEvents()).hasSize(1);
    HeatEvent event = rivalry.getHeatEvents().get(0);
    assertThat(event.getReason()).contains("Failed resolution attempt (25)");
  }

  @Test
  @DisplayName("Should not allow resolution below the floor")
  void shouldNotAllowResolutionBelowFloor() {
    rivalry.setHeat(45); // Not eligible for resolution

    boolean resolved = rivalry.attemptResolution(20, 20); // Would be successful if allowed
    assertThat(resolved).isFalse();
    assertThat(rivalry.getIsActive()).isTrue();
    assertThat(rivalry.getHeatEvents()).isEmpty(); // No event created
  }

  @Test
  @DisplayName("Should end rivalry with reason")
  void shouldEndRivalryWithReason() {
    rivalry.setHeat(15);
    assertThat(rivalry.getIsActive()).isTrue();

    Instant beforeEnd = Instant.now();
    rivalry.endRivalry("Storyline concluded");
    Instant afterEnd = Instant.now();

    assertThat(rivalry.getIsActive()).isFalse();
    assertThat(rivalry.getEndedDate()).isBetween(beforeEnd, afterEnd);

    // Check heat event was created
    assertThat(rivalry.getHeatEvents()).hasSize(1);
    HeatEvent event = rivalry.getHeatEvents().get(0);
    assertThat(event.getReason()).isEqualTo("Rivalry ended: Storyline concluded");
  }

  @Test
  @DisplayName("Should get opponent wrestler")
  void shouldGetOpponentWrestler() {
    assertThat(rivalry.getOpponent(wrestler1)).isEqualTo(wrestler2);
    assertThat(rivalry.getOpponent(wrestler2)).isEqualTo(wrestler1);
  }

  @Test
  @DisplayName("Should throw exception for non-participant wrestler")
  void shouldThrowExceptionForNonParticipantWrestler() {
    assertThatThrownBy(() -> rivalry.getOpponent(wrestler3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Wrestler is not part of this rivalry");
  }

  @Test
  @DisplayName("Should check if wrestler is involved")
  void shouldCheckIfWrestlerIsInvolved() {
    assertThat(rivalry.involvesWrestler(wrestler1)).isTrue();
    assertThat(rivalry.involvesWrestler(wrestler2)).isTrue();
    assertThat(rivalry.involvesWrestler(wrestler3)).isFalse();
  }

  @Test
  @DisplayName("Should determine rivalry intensity")
  void shouldDetermineRivalryIntensity() {
    rivalry.setHeat(5);
    assertThat(rivalry.getIntensity()).isEqualTo(RivalryIntensity.SIMMERING);

    rivalry.setHeat(35);
    assertThat(rivalry.getIntensity()).isEqualTo(RivalryIntensity.HEATED);

    rivalry.setHeat(55);
    assertThat(rivalry.getIntensity()).isEqualTo(RivalryIntensity.INTENSE);

    rivalry.setHeat(85);
    assertThat(rivalry.getIntensity()).isEqualTo(RivalryIntensity.EXPLOSIVE);
  }

  @Test
  @DisplayName("Should create display name")
  void shouldCreateDisplayName() {
    wrestler1.setName("John Cena");
    wrestler2.setName("The Rock");
    rivalry.setHeat(35);

    String displayName = rivalry.getDisplayName();
    assertThat(displayName).isEqualTo("John Cena vs The Rock (35 heat - Heated)");
  }

  @Test
  @DisplayName("Should calculate duration in days")
  void shouldCalculateDurationInDays() {
    // Same day
    assertThat(rivalry.getDurationDays()).isEqualTo(0);

    // Started 5 days ago
    rivalry.setStartedDate(Instant.now().minusSeconds(5 * 24 * 60 * 60));
    assertThat(rivalry.getDurationDays()).isEqualTo(5);

    // Ended rivalry
    rivalry.setEndedDate(Instant.now());
    assertThat(rivalry.getDurationDays()).isEqualTo(5);
  }

  @Test
  @DisplayName("canAttemptResolution(minHeat) respects configurable heat floor")
  void canAttemptResolution_configurableFloor_gatesCorrectly() {
    rivalry.setHeat(30);
    assertThat(rivalry.canAttemptResolution(30)).isTrue();
    assertThat(rivalry.canAttemptResolution(31)).isFalse();
    assertThat(rivalry.canAttemptResolution(25)).isTrue();

    rivalry.setIsActive(false);
    assertThat(rivalry.canAttemptResolution(5)).isFalse();
  }

  @Test
  @DisplayName("attemptResolution(roll1, roll2, threshold) uses custom threshold")
  void attemptResolution_customThreshold_resolvesAboveThreshold() {
    rivalry.setHeat(55);

    // Threshold 20 — total 21 resolves (floor is the INTENSE band, 50)
    assertThat(rivalry.attemptResolution(11, 10, 20)).isTrue();
    assertThat(rivalry.getIsActive()).isFalse();
  }

  @Test
  @DisplayName("attemptResolution(roll1, roll2, threshold, minHeat) uses both config values")
  void attemptResolution_customThresholdAndMinHeat_blocksAndResolves() {
    rivalry.setHeat(30);

    // minHeat=10 allows the attempt; threshold=20, total 21 resolves
    assertThat(rivalry.attemptResolution(11, 10, 20, 10)).isTrue();
    assertThat(rivalry.getIsActive()).isFalse();
  }

  @Test
  @DisplayName("attemptResolution with minHeat blocks when heat is below floor")
  void attemptResolution_blockedByMinHeat_returnsNoEventAndFalse() {
    rivalry.setHeat(8);

    // minHeat=10 blocks the attempt — no event, returns false
    boolean resolved = rivalry.attemptResolution(20, 20, 30, 10);
    assertThat(resolved).isFalse();
    assertThat(rivalry.getIsActive()).isTrue();
    assertThat(rivalry.getHeatEvents()).isEmpty();
  }

  @Test
  @DisplayName("Should handle negative heat changes")
  void shouldHandleNegativeHeatChanges() {
    rivalry.setHeat(10);
    rivalry.addHeat(-3, "Wrestlers made peace");

    assertThat(rivalry.getHeat()).isEqualTo(7);

    HeatEvent event = rivalry.getHeatEvents().get(0);
    assertThat(event.getHeatChange()).isEqualTo(-3);
    assertThat(event.getHeatAfterEvent()).isEqualTo(7);
  }

  @Test
  @DisplayName("addHeat with ceiling clamps at max and floors at zero")
  void addHeat_withCeiling_clampsBothEnds() {
    rivalry.setHeat(90);
    rivalry.addHeat(25, "Contender bonus", 100, Instant.now());
    assertThat(rivalry.getHeat()).isEqualTo(100);

    rivalry.addHeat(-140, "Blowoff vent", 100, Instant.now());
    assertThat(rivalry.getHeat()).isEqualTo(0);
  }

  @Test
  @DisplayName("addHeat without ceiling is unbounded (test-only overload)")
  void addHeat_withoutCeiling_unbounded() {
    rivalry.setHeat(95);
    rivalry.addHeat(22, "Legacy path");
    assertThat(rivalry.getHeat()).isEqualTo(117); // production once observed this value
  }

  @Test
  @DisplayName("addHeat with explicit eventDate stamps the kayfabe clock")
  void addHeat_withEventDate_stampsKayfabeTime() {
    Instant kayfabe = Instant.parse("2026-06-15T20:00:00Z");
    rivalry.addHeat(10, "Show segment", 100, kayfabe);

    HeatEvent event = rivalry.getHeatEvents().get(0);
    assertThat(event.getEventDate()).isEqualTo(kayfabe);
  }
}

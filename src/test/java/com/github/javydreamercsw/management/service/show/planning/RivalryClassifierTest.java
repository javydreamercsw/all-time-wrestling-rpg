/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pins for the shared rivalry classifier — the same rules the prompt teaches (ATW-wbch). */
class RivalryClassifierTest {

  private static ShowPlanningRivalryDTO rivalry(int heat, String lastSegmentType) {
    ShowPlanningRivalryDTO dto = new ShowPlanningRivalryDTO();
    dto.setId(1L);
    dto.setHeat(heat);
    dto.setLastSegmentType(lastSegmentType);
    return dto;
  }

  @Test
  void mustBook_belowPleEligibleHeat() {
    assertEquals(
        RivalryClassifier.MUST_BOOK,
        RivalryClassifier.classify(rivalry(13, "Tag Team"), false, false, false));
  }

  @Test
  void pleResolutionEligible_midHeat() {
    assertEquals(
        RivalryClassifier.PLE_RESOLUTION_ELIGIBLE,
        RivalryClassifier.classify(rivalry(26, "Free-for-All"), false, false, false));
  }

  @Test
  void pleResolutionRequired_maxHeatOnWeekly() {
    // A max-heat feud whose last segment was a match: reserved for the PLE.
    assertEquals(
        RivalryClassifier.PLE_RESOLUTION_REQUIRED,
        RivalryClassifier.classify(rivalry(89, "One on One"), false, false, false));
  }

  @Test
  void promoCooldown_takesPrecedenceOverHeatTiers() {
    // Even a heat-89 feud whose last segment was a Promo is in cooldown — vary the format.
    assertEquals(
        RivalryClassifier.PROMO_COOLDOWN,
        RivalryClassifier.classify(rivalry(89, "Promo"), false, false, false));
  }

  @Test
  void stipulationRequired_maxHeatOnPle() {
    assertEquals(
        RivalryClassifier.STIPULATION_REQUIRED,
        RivalryClassifier.classify(rivalry(35, "One on One"), false, false, true));
  }

  @Test
  void promoCooldown_lastSegmentWasPromoOnWeekly() {
    assertEquals(
        RivalryClassifier.PROMO_COOLDOWN,
        RivalryClassifier.classify(rivalry(15, "Promo"), false, false, false));
  }

  @Test
  void promoCooldown_doesNotApplyOnPle() {
    // On a PLE the heat rules take over (cooldown is a weekly format guard).
    assertEquals(
        RivalryClassifier.STIPULATION_REQUIRED,
        RivalryClassifier.classify(rivalry(35, "Promo"), false, false, true));
  }

  @Test
  void confrontationOnly_mixedGenderWhenIntergenderDisabled() {
    assertEquals(
        RivalryClassifier.CONFRONTATION_ONLY,
        RivalryClassifier.classify(rivalry(15, "One on One"), false, true, false));
  }

  @Test
  void confrontationOnly_overridesEverythingElse() {
    // Mixed gender + heat 40 + last was a Promo: the intergender gate fires first.
    assertEquals(
        RivalryClassifier.CONFRONTATION_ONLY,
        RivalryClassifier.classify(rivalry(40, "Promo"), false, true, false));
  }

  @Test
  void intergenderAllowed_mixedGenderIsAMatch() {
    assertEquals(
        RivalryClassifier.MUST_BOOK,
        RivalryClassifier.classify(rivalry(15, "One on One"), true, true, false));
  }

  @Test
  void isMixedGender_oneSidedRivalry_isFalse() {
    // Fewer than two participants can't resolve to two genders.
    ShowPlanningRivalryDTO single = rivalry(15, "One on One");
    single.setParticipants(List.of("Gail Kim"));

    assertFalse(RivalryClassifier.isMixedGender(single, Map.of("Gail Kim", "FEMALE")));
  }

  @Test
  void isMixedGender_nullParticipants_isFalse() {
    ShowPlanningRivalryDTO nameless = rivalry(15, "One on One");
    nameless.setParticipants(null);

    assertFalse(RivalryClassifier.isMixedGender(nameless, Map.of("Gail Kim", "FEMALE")));
  }

  @Test
  void isMixedGender_participantsMissingFromMap_contributeNothing() {
    // Unknown names (hand-built contexts) pass the gate — only resolvable genders count.
    ShowPlanningRivalryDTO unknown = rivalry(15, "One on One");
    unknown.setParticipants(List.of("Gail Kim", "André the Giant"));

    assertFalse(RivalryClassifier.isMixedGender(unknown, Map.of()));
  }

  @Test
  void isMixedGender_twoResolvableGenders_isTrue() {
    ShowPlanningRivalryDTO mixed = rivalry(15, "One on One");
    mixed.setParticipants(List.of("Gail Kim", "André the Giant"));

    assertTrue(
        RivalryClassifier.isMixedGender(
            mixed, Map.of("Gail Kim", "FEMALE", "André the Giant", "MALE")));
  }
}

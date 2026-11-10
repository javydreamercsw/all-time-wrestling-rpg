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

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import java.util.Map;
import java.util.Set;

/**
 * The rivalry classification switch, shared by the prompt builder (advisory labels for the AI) and
 * the local show planner (constructive selection — ATW-eqo0 successor). One implementation so both
 * consumers agree by construction: the planner books exactly the doctrine the prompt describes.
 *
 * <p>Classification order mirrors the prompt's rules text:
 *
 * <ol>
 *   <li>{@code CONFRONTATION_ONLY} — mixed gender while intergender is disabled: a match would
 *       violate the restriction; the feud advances via non-match segments.
 *   <li>{@code PROMO_COOLDOWN} — the feud's last completed segment was a Promo (non-PLE): another
 *       Promo books the same talking segment show after show; vary the format.
 *   <li>{@code STIPULATION_REQUIRED} (PLE) / {@code PLE_RESOLUTION_REQUIRED} (weekly) — heat ≥ 30:
 *       max-heat feuds get their decisive stipulation match at the PLE, not before.
 *   <li>{@code PLE_RESOLUTION_ELIGIBLE} — heat 20–29.
 *   <li>{@code MUST_BOOK} — heat 10–19: must have a segment on this show.
 * </ol>
 */
public final class RivalryClassifier {

  public static final String CONFRONTATION_ONLY = "CONFRONTATION_ONLY";
  public static final String PROMO_COOLDOWN = "PROMO_COOLDOWN";
  public static final String STIPULATION_REQUIRED = "STIPULATION_REQUIRED";
  public static final String PLE_RESOLUTION_REQUIRED = "PLE_RESOLUTION_REQUIRED";
  public static final String PLE_RESOLUTION_ELIGIBLE = "PLE_RESOLUTION_ELIGIBLE";
  public static final String MUST_BOOK = "MUST_BOOK";

  /** Heat at which a rivalry is reserved for PLE resolution (stipulation match at the PLE). */
  static final int PLE_RESOLUTION_HEAT = 30;

  /** Heat at which a rivalry is strong enough to headline a PLE when it gets there. */
  static final int PLE_ELIGIBLE_HEAT = 20;

  private RivalryClassifier() {}

  /**
   * Classifies one rivalry against its show's context. The one entry point both consumers use.
   *
   * @param rivalry the rivalry to classify
   * @param intergenderAllowed whether the universe allows intergender matches
   * @param mixedGender whether the rivalry's participants resolve to more than one gender
   * @param premiumLiveEvent whether the show being planned is a PLE
   */
  public static String classify(
      ShowPlanningRivalryDTO rivalry,
      boolean intergenderAllowed,
      boolean mixedGender,
      boolean premiumLiveEvent) {
    if (!intergenderAllowed && mixedGender) {
      return CONFRONTATION_ONLY;
    }
    if ("Promo".equals(rivalry.getLastSegmentType()) && !premiumLiveEvent) {
      return PROMO_COOLDOWN;
    }
    if (rivalry.getHeat() >= PLE_RESOLUTION_HEAT) {
      return premiumLiveEvent ? STIPULATION_REQUIRED : PLE_RESOLUTION_REQUIRED;
    }
    if (rivalry.getHeat() >= PLE_ELIGIBLE_HEAT) {
      return PLE_RESOLUTION_ELIGIBLE;
    }
    return MUST_BOOK;
  }

  /**
   * Whether the rivalry's two participants resolve to more than one gender against {@code
   * genderByName} (sanitized-name → gender). Participants missing from the map contribute nothing.
   */
  public static boolean isMixedGender(
      ShowPlanningRivalryDTO rivalry, Map<String, String> genderByName) {
    if (rivalry.getParticipants() == null || rivalry.getParticipants().size() < 2) {
      return false;
    }
    Set<String> genders =
        rivalry.getParticipants().stream()
            .map(genderByName::get)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    return genders.size() > 1;
  }
}

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
package com.github.javydreamercsw.management.sync.review;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry of the seed-provenance manifest's domain keys — the single list of every domain the sync
 * pipeline stamps into {@code seed-provenance.json}.
 *
 * <p>Consumers: {@code SeedProvenanceManifestEnforcementTest} asserts the committed manifest covers
 * every domain listed here, and future code (e.g. new syncs registering a manifest domain) should
 * iterate this set instead of hardcoding domain keys. When a new sync starts stamping a domain, add
 * it here, to {@link GenerateSeedProvenanceManifest}, and regenerate the manifest.
 *
 * <p>Backed by a {@link LinkedHashSet} so iteration is deterministic (manifest document order) and
 * membership checks stay O(1); wrapped unmodifiable so the registry cannot be mutated at runtime.
 */
public final class SeedDomains {

  /**
   * Every domain the committed manifest must cover, in manifest document order: achievement, arena,
   * campaign_ability_card, campaign_upgrade, card, card_set, championship, commentator,
   * commentary_team, deck, faction, location, npc, outcome_matrix, ringside_action, segment_rule,
   * segment_type, show_template, show_type, status_card, team, tournament, wrestler,
   * wrestler_relationship.
   */
  public static final Set<String> STAMPING_DOMAINS =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              List.of(
                  "achievement",
                  "arena",
                  "campaign_ability_card",
                  "campaign_upgrade",
                  "card",
                  "card_set",
                  "championship",
                  "commentator",
                  "commentary_team",
                  "deck",
                  "faction",
                  "location",
                  "npc",
                  "outcome_matrix",
                  "ringside_action",
                  "segment_rule",
                  "segment_type",
                  "show_template",
                  "show_type",
                  "status_card",
                  "team",
                  "tournament",
                  "wrestler",
                  "wrestler_relationship")));

  private SeedDomains() {}
}

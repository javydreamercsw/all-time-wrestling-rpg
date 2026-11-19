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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to faction rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code faction:NAME-slug} via {@link FactionSync#entityKeyFor}) — the name
 * is the domain's stable identity, and key-based reminting keeps resolution rename-proof if seed
 * naming ever moves; the {@code entity_display_name} stays the exact seed name for the review UI
 * and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class FactionSeedApplier implements SeedDomainApplier {

  private final FactionRepository factionRepository;
  private final FactionRivalryRepository factionRivalryRepository;
  private final TeamRepository teamRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final NpcRepository npcRepository;

  public FactionSeedApplier(
      final FactionRepository factionRepository,
      final FactionRivalryRepository factionRivalryRepository,
      final TeamRepository teamRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final NpcRepository npcRepository) {
    this.factionRepository = factionRepository;
    this.factionRivalryRepository = factionRivalryRepository;
    this.teamRepository = teamRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.npcRepository = npcRepository;
  }

  @Override
  public String domainKey() {
    return FactionSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findFaction(decision)
        .ifPresentOrElse(
            faction -> stamp(faction, decision),
            () ->
                log.warn(
                    "Seed decision {}: faction '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findFaction(decision)
        .ifPresentOrElse(
            faction -> {
              applyFieldValue(faction, decision);
              stamp(faction, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: faction '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findFaction(decision)
        .ifPresentOrElse(
            faction -> {
              faction.setSeedFingerprint(null);
              factionRepository.save(faction);
            },
            () ->
                log.warn(
                    "Seed decision {}: faction '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (member states, teams,
    // rivalries). The fail-safe direction re-checks at delete time: a faction with any of them is
    // kept as custom data rather than blowing the FK mid-apply.
    findFaction(decision)
        .ifPresentOrElse(
            faction -> {
              if (hasDependents(faction)) {
                log.warn(
                    "Seed decision {}: faction '{}' still has members, teams, or rivalries — the"
                        + " row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                factionRepository.delete(faction);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: faction '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findFactionByKey(entityKey).map(this::hasDependents).orElse(false);
  }

  /**
   * The faction's REAL persisted FK relationships (verified against the entity model): member
   * wrestler states ({@code WrestlerState.faction}), teams ({@code Team.faction}), and faction
   * rivalries ({@code FactionRivalry.faction1/faction2}).
   */
  private boolean hasDependents(Faction faction) {
    // findByFaction already filters states by the faction FK — no redundant equals re-check.
    return !wrestlerStateRepository.findByFaction(faction).isEmpty()
        || !teamRepository.findByFaction(faction).isEmpty()
        || !factionRivalryRepository.findByFaction1OrFaction2(faction, faction).isEmpty();
  }

  /** Resolves the decision's faction by reminting its entity key. */
  private Optional<Faction> findFaction(SeedUpdateDecision decision) {
    return findFactionByKey(decision.getEntityKey());
  }

  private Optional<Faction> findFactionByKey(String entityKey) {
    return factionRepository.findAll().stream()
        .filter(f -> entityKey.equals(FactionSync.entityKeyFor(f.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Faction faction, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> faction.setDescription(value);
      case "managerName" -> {
        if (value == null || value.isBlank()) {
          faction.setManager(null);
        } else {
          npcRepository
              .findByName(value)
              .ifPresentOrElse(
                  faction::setManager,
                  () ->
                      log.warn(
                          "Seed decision {}: NPC manager '{}' not found — manager reference not"
                              + " changed on faction '{}'",
                          decision.getId(),
                          value,
                          decision.getEntityDisplayName()));
        }
      }
      case "affinity" -> faction.setAffinity(value == null ? 0 : Integer.parseInt(value));
      default ->
          throw new IllegalStateException(
              "Faction field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    factionRepository.save(faction);
  }

  private void stamp(Faction faction, SeedUpdateDecision decision) {
    faction.setSeedFingerprint(decision.getNewFingerprint());
    factionRepository.save(faction);
  }
}

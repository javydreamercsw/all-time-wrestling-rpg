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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to NPC rows (seed-update review feature). The entity
 * is resolved through the decision row's {@code entity_display_name} — the exact seed name captured
 * at detection time (slugs are lossy: "Rezan the Resolved" downcases mid-name words harmlessly, but
 * "Tomás “Kid Whistle” Iglesias" destroys the curly quotes and diacritics, and the repository
 * lookups are case-sensitive).
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class NpcSeedApplier implements SeedDomainApplier {

  private final NpcRepository npcRepository;
  private final NpcService npcService;
  private final SegmentRepository segmentRepository;
  private final CommentatorRepository commentatorRepository;
  private final FactionRepository factionRepository;
  private final TeamRepository teamRepository;
  private final WrestlerStateRepository wrestlerStateRepository;

  public NpcSeedApplier(
      final NpcRepository npcRepository,
      final NpcService npcService,
      final SegmentRepository segmentRepository,
      final CommentatorRepository commentatorRepository,
      final FactionRepository factionRepository,
      final TeamRepository teamRepository,
      final WrestlerStateRepository wrestlerStateRepository) {
    this.npcRepository = npcRepository;
    this.npcService = npcService;
    this.segmentRepository = segmentRepository;
    this.commentatorRepository = commentatorRepository;
    this.factionRepository = factionRepository;
    this.teamRepository = teamRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
  }

  @Override
  public String domainKey() {
    return NpcSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    npcRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            npc -> stamp(npc, decision),
            () ->
                log.warn(
                    "Seed decision {}: npc '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    npcRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            npc -> {
              applyFieldValue(npc, decision);
              stamp(npc, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: npc '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    npcRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            npc -> {
              npc.setSeedFingerprint(null);
              npcService.save(npc);
            },
            () ->
                log.warn(
                    "Seed decision {}: npc '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (segments refereed,
    // commentator rows, faction/team managers, wrestler states). A manager/referee NPC with
    // references would fail its FK on delete, so the fail-safe direction re-checks at delete
    // time: a referenced NPC is kept as custom data.
    npcRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            npc -> {
              if (hasDependents(NpcSync.entityKeyFor(npc.getName()))) {
                log.warn(
                    "Seed decision {}: npc '{}' still has references (segments, commentators,"
                        + " factions, teams, or wrestler states) — the row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                npcRepository.delete(npc);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: npc '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous (existing hyphens in a
    // seed name are indistinguishable from slug-converted spaces), so match the key exactly the
    // way detection minted it.
    return npcRepository.findAll().stream()
        .filter(n -> entityKey.equals(NpcSync.entityKeyFor(n.getName())))
        .findFirst()
        .map(this::hasReferences)
        .orElse(false);
  }

  /** True when any of the five real NPC FK holders (see class doc) points at the npc. */
  private boolean hasReferences(Npc npc) {
    Long npcId = npc.getId();
    return !segmentRepository.findByReferee(npc).isEmpty()
        || commentatorRepository.findAll().stream().anyMatch(c -> npcId.equals(c.getNpc().getId()))
        || factionRepository.findAll().stream()
            .anyMatch(f -> f.getManager() != null && npcId.equals(f.getManager().getId()))
        || teamRepository.findAll().stream()
            .anyMatch(t -> t.getManager() != null && npcId.equals(t.getManager().getId()))
        || wrestlerStateRepository.findAll().stream()
            .anyMatch(s -> s.getManager() != null && npcId.equals(s.getManager().getId()));
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Npc npc, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> npc.setDescription(value);
      case "npcType" -> npc.setNpcType(value);
      case "expansionCode" -> npc.setExpansionCode(value);
      case "alignment" ->
          npc.setAlignment(value == null ? AlignmentType.NEUTRAL : AlignmentType.valueOf(value));
      case "awareness" -> npcService.setAwareness(npc, value == null ? 0 : Integer.parseInt(value));
      default ->
          throw new IllegalStateException(
              "Npc field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    npcService.save(npc);
  }

  private void stamp(Npc npc, SeedUpdateDecision decision) {
    npc.setSeedFingerprint(decision.getNewFingerprint());
    npcService.save(npc);
  }
}

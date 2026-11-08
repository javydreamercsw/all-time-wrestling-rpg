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

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to arena rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code arena:NAME-slug} via {@link ArenaSync#entityKeyFor}) — the name is
 * the domain's stable identity, and key-based reminting keeps resolution rename-proof if seed
 * naming ever moves; the {@code entity_display_name} stays the exact seed name for the review UI
 * and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class ArenaSeedApplier implements SeedDomainApplier {

  private final ArenaRepository arenaRepository;
  private final LocationRepository locationRepository;
  private final ShowRepository showRepository;

  public ArenaSeedApplier(
      final ArenaRepository arenaRepository,
      final LocationRepository locationRepository,
      final ShowRepository showRepository) {
    this.arenaRepository = arenaRepository;
    this.locationRepository = locationRepository;
    this.showRepository = showRepository;
  }

  @Override
  public String domainKey() {
    return ArenaSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findArena(decision)
        .ifPresentOrElse(
            arena -> stamp(arena, decision),
            () ->
                log.warn(
                    "Seed decision {}: arena '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findArena(decision)
        .ifPresentOrElse(
            arena -> {
              applyFieldValue(arena, decision);
              stamp(arena, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: arena '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findArena(decision)
        .ifPresentOrElse(
            arena -> {
              arena.setSeedFingerprint(null);
              arenaRepository.save(arena);
            },
            () ->
                log.warn(
                    "Seed decision {}: arena '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (shows booked there).
    // The fail-safe direction re-checks at delete time: an arena with booked shows is kept as
    // custom data rather than blowing the FK mid-apply.
    findArena(decision)
        .ifPresentOrElse(
            arena -> {
              if (showRepository.existsByArena(arena)) {
                log.warn(
                    "Seed decision {}: arena '{}' still has booked shows — the row is kept as"
                        + " custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                arenaRepository.delete(arena);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: arena '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findArenaByKey(entityKey)
        .map(arena -> showRepository.existsByArena(arena))
        .orElse(false);
  }

  /**
   * Resolves the decision's arena by reminting its entity key — the key survives the admin rename a
   * tier-2 NAME collision parks around (see class doc).
   */
  private Optional<Arena> findArena(SeedUpdateDecision decision) {
    return findArenaByKey(decision.getEntityKey());
  }

  private Optional<Arena> findArenaByKey(String entityKey) {
    return arenaRepository.findAll().stream()
        .filter(a -> entityKey.equals(ArenaSync.entityKeyFor(a.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Arena arena, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> arena.setDescription(value);
      case "location" ->
          locationRepository
              .findByName(value)
              .ifPresentOrElse(
                  arena::setLocation,
                  () ->
                      log.warn(
                          "Seed decision {}: location '{}' not found — location reference not"
                              + " changed on arena '{}'",
                          decision.getId(),
                          value,
                          decision.getEntityDisplayName()));
      case "capacity" -> arena.setCapacity(value == null ? 0 : Integer.parseInt(value));
      case "alignmentBias" ->
          arena.setAlignmentBias(
              value == null ? Arena.AlignmentBias.NEUTRAL : Arena.AlignmentBias.valueOf(value));
      case "imageUrl" -> arena.setImageUrl(value);
      case "environmentalTraits" -> arena.setEnvironmentalTraits(splitLines(value));
      default ->
          throw new IllegalStateException(
              "Arena field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    arenaRepository.save(arena);
  }

  /** Splits the canonical newline-joined trait serialization back into the row's trait set. */
  private Set<String> splitLines(String value) {
    Set<String> traits = new HashSet<>();
    if (value != null && !value.isBlank()) {
      for (String line : value.split("\n")) {
        if (!line.isBlank()) {
          traits.add(line);
        }
      }
    }
    return traits;
  }

  private void stamp(Arena arena, SeedUpdateDecision decision) {
    arena.setSeedFingerprint(decision.getNewFingerprint());
    arenaRepository.save(arena);
  }
}

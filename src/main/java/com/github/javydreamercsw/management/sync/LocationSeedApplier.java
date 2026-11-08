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
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to location rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code location:NAME-slug} via {@link LocationSync#entityKeyFor}) — the name
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
public class LocationSeedApplier implements SeedDomainApplier {

  private final LocationRepository locationRepository;
  private final ArenaRepository arenaRepository;

  public LocationSeedApplier(
      final LocationRepository locationRepository, final ArenaRepository arenaRepository) {
    this.locationRepository = locationRepository;
    this.arenaRepository = arenaRepository;
  }

  @Override
  public String domainKey() {
    return LocationSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findLocation(decision)
        .ifPresentOrElse(
            location -> stamp(location, decision),
            () ->
                log.warn(
                    "Seed decision {}: location '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findLocation(decision)
        .ifPresentOrElse(
            location -> {
              applyFieldValue(location, decision);
              stamp(location, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: location '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findLocation(decision)
        .ifPresentOrElse(
            location -> {
              location.setSeedFingerprint(null);
              locationRepository.save(location);
            },
            () ->
                log.warn(
                    "Seed decision {}: location '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (arenas sited there).
    // The fail-safe direction re-checks at delete time: a location with arenas is kept as custom
    // data rather than blowing the FK mid-apply.
    findLocation(decision)
        .ifPresentOrElse(
            location -> {
              if (arenaRepository.existsByLocation(location)) {
                log.warn(
                    "Seed decision {}: location '{}' still has arenas — the row is kept as custom"
                        + " data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                locationRepository.delete(location);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: location '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findLocationByKey(entityKey)
        .map(location -> arenaRepository.existsByLocation(location))
        .orElse(false);
  }

  /**
   * Resolves the decision's location by reminting its entity key — the key survives the admin
   * rename a tier-2 NAME collision parks around (see class doc).
   */
  private Optional<Location> findLocation(SeedUpdateDecision decision) {
    return findLocationByKey(decision.getEntityKey());
  }

  private Optional<Location> findLocationByKey(String entityKey) {
    return locationRepository.findAll().stream()
        .filter(l -> entityKey.equals(LocationSync.entityKeyFor(l.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Location location, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> location.setDescription(value);
      case "imageUrl" -> location.setImageUrl(value);
      case "culturalTags" -> location.setCulturalTags(splitLines(value));
      default ->
          throw new IllegalStateException(
              "Location field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    locationRepository.save(location);
  }

  /** Splits the canonical newline-joined tag serialization back into the row's tag set. */
  private java.util.Set<String> splitLines(String value) {
    java.util.Set<String> tags = new java.util.HashSet<>();
    if (value != null && !value.isBlank()) {
      for (String line : value.split("\n")) {
        if (!line.isBlank()) {
          tags.add(line);
        }
      }
    }
    return tags;
  }

  private void stamp(Location location, SeedUpdateDecision decision) {
    location.setSeedFingerprint(decision.getNewFingerprint());
    locationRepository.save(location);
  }
}

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.dto.LocationImportDTO;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
public class LocationSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "location";

  private final SeedSyncSupport syncSupport;
  private final LocationRepository locationRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public LocationSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final LocationRepository locationRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.locationRepository = locationRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && locationRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("locations.json");
    if (resource.exists()) {
      log.debug("Loading locations from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<LocationImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (dtos == null) {
          return;
        }
        List<Location> toSave = new ArrayList<>();
        log.debug("Found {} locations in JSON file", dtos.size());
        for (LocationImportDTO dto : dtos) {
          Optional<Location> existing = locationRepository.findByName(dto.getName());
          if (existing.isEmpty()) {
            Location location =
                Location.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .imageUrl(dto.getImageUrl())
                    .culturalTags(dto.getCulturalTags())
                    .build();
            // Stamp the freshly seeded row so verify-mode-style checks and the version-change
            // detection pass can match it against the manifest.
            location.setSeedFingerprint(locationFingerprint(location));
            toSave.add(location);
          } else {
            Location loc = existing.get();
            boolean changed = false;
            if (!dto.getDescription().equals(loc.getDescription())) {
              loc.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getImageUrl() != null && !dto.getImageUrl().equals(loc.getImageUrl())) {
              loc.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (dto.getCulturalTags() != null
                && !dto.getCulturalTags().equals(loc.getCulturalTags())) {
              loc.setCulturalTags(dto.getCulturalTags());
              changed = true;
            }
            if (changed) {
              loc.setSeedFingerprint(locationFingerprint(loc));
              toSave.add(loc);
            }
          }
        }
        locationRepository.saveAll(toSave);
        locationRepository.flush();
        log.debug("Location loading completed - {} locations processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading locations from file", e);
      }
    } else {
      log.warn("Locations file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing locations are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || locationRepository.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("locations.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<LocationImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      if (dtos == null) {
        return;
      }
      Map<String, Location> existingByName =
          locationRepository.findAll().stream()
              .collect(Collectors.toMap(Location::getName, l -> l, (a, b) -> a));
      List<Location> toSave = new ArrayList<>();
      for (LocationImportDTO dto : dtos) {
        Location existing = existingByName.get(dto.getName());
        if (existing == null) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(existing, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getName()),
                dto.getName(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::locationValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(existing, dto, result.silentUpdateFields());
          toSave.add(existing);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(existing.getSeedFingerprint())) {
          existing.setSeedFingerprint(stamp);
          if (!toSave.contains(existing)) {
            toSave.add(existing);
          }
        }
      }
      if (!toSave.isEmpty()) {
        locationRepository.saveAll(toSave);
        locationRepository.flush();
      }
      log.info(
          "Seed review detection (location) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during location seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one location row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Location existing, LocationImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "imageUrl", null, existing.getImageUrl(), dto.getImageUrl()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "culturalTags",
            null,
            tagsCanonical(existing.getCulturalTags()),
            tagsCanonical(dto.getCulturalTags())));
    return obs;
  }

  /**
   * Canonical tags-set serialization: sorted, newline-joined (the persisted {@code
   * ElementCollection} set has no order guarantee across restarts, so both sides sort before
   * hashing).
   */
  private String tagsCanonical(Set<String> tags) {
    return tags == null ? "" : tags.stream().sorted().collect(Collectors.joining("\n"));
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(LocationImportDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(), dto.getImageUrl(), tagsCanonical(dto.getCulturalTags()));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #locationFingerprint(Location)} exactly: the observed strings hash identically
   * to the typed values (null serializes as "null" both ways).
   */
  private String locationValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1), values.get(2));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a location name — shared with {@code LocationSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "location:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a location row, in fingerprint order: description,
   * imageUrl, culturalTags (sorted, newline-joined). {@code active} is admin-managed and excluded
   * (the seed write never touches it). Public for the seed-provenance manifest generator, which
   * re-hashes fresh seed rows through the owning sync's own method.
   */
  public String locationFingerprint(Location location) {
    return syncSupport.fingerprint(
        location.getDescription(),
        location.getImageUrl(),
        tagsCanonical(location.getCulturalTags()));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror the FULL-mode write set (only non-null seed values overwrite).
   */
  private void applySilentSeedFields(
      Location existing, LocationImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "imageUrl" -> {
          if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
          }
        }
        case "culturalTags" -> {
          if (dto.getCulturalTags() != null) {
            existing.setCulturalTags(dto.getCulturalTags());
          }
        }
        default ->
            throw new IllegalStateException("Unknown silent-update location field '" + field + "'");
      }
    }
  }
}

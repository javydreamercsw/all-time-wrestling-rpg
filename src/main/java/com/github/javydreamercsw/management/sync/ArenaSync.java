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
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.dto.ArenaImportDTO;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(110)
public class ArenaSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "arena";

  private final SeedSyncSupport syncSupport;
  private final ArenaRepository arenaRepository;
  private final LocationRepository locationRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public ArenaSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final ArenaRepository arenaRepository,
      final LocationRepository locationRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.arenaRepository = arenaRepository;
    this.locationRepository = locationRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && arenaRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("arenas.json");
    if (resource.exists()) {
      log.debug("Loading arenas from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ArenaImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (dtos == null) {
          return;
        }
        Map<String, Arena> existingByName =
            arenaRepository.findAllWithLocation().stream()
                .collect(Collectors.toMap(Arena::getName, a -> a));
        List<Arena> toSave = new ArrayList<>();
        log.debug("Found {} arenas in JSON file", dtos.size());
        for (ArenaImportDTO dto : dtos) {
          Optional<Arena> existingArena = Optional.ofNullable(existingByName.get(dto.getName()));
          if (existingArena.isEmpty()) {
            Optional<Location> location = locationRepository.findByName(dto.getLocation());
            if (location.isPresent()) {
              Arena arena =
                  Arena.builder()
                      .name(dto.getName())
                      .description(dto.getDescription())
                      .location(location.get())
                      .capacity(dto.getCapacity())
                      .alignmentBias(dto.getAlignmentBias())
                      .imageUrl(dto.getImageUrl())
                      .environmentalTraits(dto.getEnvironmentalTraits())
                      .build();
              // Stamp the freshly seeded row so verify-mode-style checks and the version-change
              // detection pass can match it against the manifest.
              arena.setSeedFingerprint(arenaFingerprint(arena));
              toSave.add(arena);
            }
          } else {
            Arena existing = existingArena.get();
            boolean changed = false;
            if (!Objects.equals(dto.getDescription(), existing.getDescription())) {
              existing.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getCapacity() != existing.getCapacity()) {
              existing.setCapacity(dto.getCapacity());
              changed = true;
            }
            if (dto.getAlignmentBias() != existing.getAlignmentBias()) {
              existing.setAlignmentBias(dto.getAlignmentBias());
              changed = true;
            }
            if (!Objects.equals(dto.getImageUrl(), existing.getImageUrl())) {
              existing.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (!Objects.equals(dto.getEnvironmentalTraits(), existing.getEnvironmentalTraits())) {
              existing.setEnvironmentalTraits(dto.getEnvironmentalTraits());
              changed = true;
            }
            Optional<Location> locationOpt = locationRepository.findByName(dto.getLocation());
            if (locationOpt.isPresent() && !locationOpt.get().equals(existing.getLocation())) {
              existing.setLocation(locationOpt.get());
              changed = true;
            }
            if (changed) {
              existing.setSeedFingerprint(arenaFingerprint(existing));
              toSave.add(existing);
            }
          }
        }
        arenaRepository.saveAll(toSave);
        log.debug("Arena loading completed - {} arenas processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading arenas from file", e);
      }
    } else {
      log.warn("Arenas file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing arenas are classified field-by-field through
   * {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields update
   * silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay untouched.
   * Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already applies the seed
   * everywhere; an empty DB has nothing to collide).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || arenaRepository.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("arenas.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<ArenaImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      if (dtos == null) {
        return;
      }
      Map<String, Arena> existingByName =
          arenaRepository.findAllWithLocation().stream()
              .collect(Collectors.toMap(Arena::getName, a -> a, (a, b) -> a));
      List<Arena> toSave = new ArrayList<>();
      for (ArenaImportDTO dto : dtos) {
        Arena existing = existingByName.get(dto.getName());
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
                this::arenaValueListFingerprint,
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
        arenaRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (arena) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during arena seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one arena row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Arena existing, ArenaImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "location",
            null,
            existing.getLocation() == null ? null : existing.getLocation().getName(),
            dto.getLocation()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "capacity",
            null,
            existing.getCapacity() == null ? null : existing.getCapacity().toString(),
            dto.getCapacity() == null ? null : dto.getCapacity().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "alignmentBias",
            null,
            existing.getAlignmentBias() == null ? null : existing.getAlignmentBias().name(),
            dto.getAlignmentBias() == null ? null : dto.getAlignmentBias().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "imageUrl", null, existing.getImageUrl(), dto.getImageUrl()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "environmentalTraits",
            null,
            traitsCanonical(existing.getEnvironmentalTraits()),
            traitsCanonical(dto.getEnvironmentalTraits())));
    return obs;
  }

  /**
   * Canonical trait-set serialization: sorted, newline-joined (the persisted {@code
   * ElementCollection} set has no order guarantee across restarts, so both sides sort before
   * hashing).
   */
  private String traitsCanonical(java.util.Set<String> traits) {
    return traits == null ? "" : traits.stream().sorted().collect(Collectors.joining("\n"));
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(ArenaImportDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getLocation(),
        dto.getCapacity(),
        dto.getAlignmentBias() == null ? null : dto.getAlignmentBias().name(),
        dto.getImageUrl(),
        traitsCanonical(dto.getEnvironmentalTraits()));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #arenaFingerprint(Arena)} exactly: the observed strings hash identically to the
   * typed values (see WrestlerSync's raw pass-through shape).
   */
  private String arenaValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for an arena name — shared with {@code ArenaSeedApplier} so apply-side
   * resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "arena:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for an arena row, in fingerprint order: description,
   * locationName, capacity, alignmentBias, imageUrl, environmentalTraits (sorted, newline-joined).
   * {@code active} is admin-managed and excluded. Public for the seed-provenance manifest
   * generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String arenaFingerprint(Arena arena) {
    return syncSupport.fingerprint(
        arena.getDescription(),
        arena.getLocation() == null ? null : arena.getLocation().getName(),
        arena.getCapacity(),
        arena.getAlignmentBias() == null ? null : arena.getAlignmentBias().name(),
        arena.getImageUrl(),
        traitsCanonical(arena.getEnvironmentalTraits()));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the
   * location reference resolves by name through the same lookup the seed write uses (unknown names
   * log and skip, mirroring the FULL path's tolerance).
   */
  private void applySilentSeedFields(
      Arena existing, ArenaImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "location" ->
            locationRepository
                .findByName(dto.getLocation())
                .ifPresentOrElse(
                    existing::setLocation,
                    () ->
                        log.warn(
                            "Location '{}' not found — location reference not changed on arena"
                                + " '{}'",
                            dto.getLocation(),
                            dto.getName()));
        case "capacity" -> {
          if (dto.getCapacity() != null) {
            existing.setCapacity(dto.getCapacity());
          }
        }
        case "alignmentBias" -> {
          if (dto.getAlignmentBias() != null) {
            existing.setAlignmentBias(dto.getAlignmentBias());
          }
        }
        case "imageUrl" -> existing.setImageUrl(dto.getImageUrl());
        case "environmentalTraits" -> {
          if (dto.getEnvironmentalTraits() != null) {
            existing.setEnvironmentalTraits(dto.getEnvironmentalTraits());
          }
        }
        default ->
            throw new IllegalStateException("Unknown silent-update arena field '" + field + "'");
      }
    }
  }
}

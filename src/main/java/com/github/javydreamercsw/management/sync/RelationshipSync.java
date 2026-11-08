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
import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.RelationshipImportDTO;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
@Order(130)
public class RelationshipSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "wrestler_relationship";

  private final SeedSyncSupport syncSupport;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerRelationshipRepository relationshipRepository;
  private final WrestlerRelationshipService relationshipService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public RelationshipSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final WrestlerRepository wrestlerRepository,
      final WrestlerRelationshipRepository relationshipRepository,
      final WrestlerRelationshipService relationshipService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.wrestlerRepository = wrestlerRepository;
    this.relationshipRepository = relationshipRepository;
    this.relationshipService = relationshipService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && relationshipService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("relationships.json");
    if (resource.exists()) {
      log.debug("Loading relationships from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RelationshipImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        for (RelationshipImportDTO dto : dtos) {
          Optional<Wrestler> w1 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler1()));
          Optional<Wrestler> w2 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler2()));
          if (w1.isPresent() && w2.isPresent()) {
            relationshipService.createOrUpdateRelationship(
                w1.get().getId(),
                w2.get().getId(),
                dto.getType(),
                dto.getLevel(),
                dto.getIsStoryline(),
                dto.getNotes());
            // Stamp the freshly seeded row so verify-mode-style checks and the version-change
            // detection pass can match it against the manifest. The service's save leaves the
            // stamp unset; the seed write only stores rows whose pair resolves, so the just-written
            // row is the one found between the two wrestlers of this seed type.
            List<WrestlerRelationship> between =
                relationshipRepository.findBetweenWrestlers(w1.get(), w2.get());
            between.stream()
                .filter(r -> r.getType() == dto.getType())
                .filter(r -> SeedSyncSupport.isBlankFingerprint(r.getSeedFingerprint()))
                .findFirst()
                .ifPresent(
                    r -> {
                      r.setSeedFingerprint(relationshipFingerprint(r));
                      relationshipRepository.save(r);
                    });
          }
        }
        log.debug("Relationship loading completed - {} relationships processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading relationships from file", e);
      }
    } else {
      log.warn("Relationships file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing wrestler relationships are classified
   * field-by-field through {@link SeedCollisionDetector} instead of being reconciled directly —
   * tier-1 fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3
   * fields stay untouched. Runs only in PROTECT/VERIFY mode with existing rows.
   *
   * <p>The relationship's identity is the (wrestler1, wrestler2, type) triple — the seed lookup key
   * — and the seed-owned reviewable surface is level/isStoryline/notes. A relationship is gameplay
   * history in motion (the service raises levels on shared segments); a tier-2 park lets the
   * administrator choose between the seed's rebalance and the accumulated in-game state.
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || relationshipService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("relationships.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RelationshipImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      List<WrestlerRelationship> all = relationshipRepository.findAllWithWrestlers();
      List<WrestlerRelationship> toSave = new ArrayList<>();
      for (RelationshipImportDTO dto : dtos) {
        Wrestler w1 = wrestlersByName.get(dto.getWrestler1());
        Wrestler w2 = wrestlersByName.get(dto.getWrestler2());
        if (w1 == null || w2 == null) {
          continue; // seed rows pointing at unknown wrestlers can never have persisted
        }
        Optional<WrestlerRelationship> existing =
            all.stream()
                .filter(r -> r.getType() == dto.getType())
                .filter(
                    r ->
                        (r.getWrestler1().equals(w1) && r.getWrestler2().equals(w2))
                            || (r.getWrestler1().equals(w2) && r.getWrestler2().equals(w1)))
                .findFirst();
        if (existing.isEmpty()) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        WrestlerRelationship relationship = existing.get();
        String seedName = displayName(w1, w2, dto.getType());
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, seedName, seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(relationship.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(relationship, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(w1, w2, dto.getType()),
                seedName,
                rowHasStoredStamp,
                relationship.getSeedFingerprint(),
                observations,
                this::relationshipValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(relationship, dto, result.silentUpdateFields());
          toSave.add(relationship);
        }
        // Stamp-only rows must persist the re-stamp too — capture the change BEFORE setting it.
        if (stamp != null && !stamp.equals(relationship.getSeedFingerprint())) {
          relationship.setSeedFingerprint(stamp);
          if (!toSave.contains(relationship)) {
            toSave.add(relationship);
          }
        }
      }
      if (!toSave.isEmpty()) {
        relationshipRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (wrestler relationship) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during wrestler relationship seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one relationship row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      WrestlerRelationship existing, RelationshipImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "level",
            null,
            existing.getLevel() == null ? null : Integer.toString(existing.getLevel()),
            dto.getLevel() == null ? null : Integer.toString(dto.getLevel())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "isStoryline",
            null,
            Boolean.toString(existing.getIsStoryline()),
            Boolean.toString(dto.getIsStoryline())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "notes", null, existing.getNotes(), dto.getNotes()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(RelationshipImportDTO dto) {
    return syncSupport.fingerprint(
        dto.getLevel() == null ? null : Integer.toString(dto.getLevel()),
        Boolean.toString(dto.getIsStoryline()),
        dto.getNotes());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #relationshipFingerprint(WrestlerRelationship)} exactly (see WrestlerSync's raw
   * pass-through shape).
   */
  private String relationshipValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1), values.get(2));
  }

  /** Human-readable seed name for a relationship row: "W1 <type> W2" in seed file order. */
  private static String displayName(Wrestler w1, Wrestler w2, RelationshipType type) {
    return relationshipSeedKeyFor(w1.getName(), w2.getName(), type);
  }

  /**
   * The seed key for a relationship row — "W1 <TYPE> W2" in seed-file order. Public shared with the
   * seed-provenance manifest generator, which keys rows identically.
   */
  public static String relationshipSeedKeyFor(String name1, String name2, RelationshipType type) {
    return name1 + " <" + type.name() + "> " + name2;
  }

  private static String entityKey(Wrestler w1, Wrestler w2, RelationshipType type) {
    return entityKeyFor(w1.getName(), w2.getName(), type);
  }

  /**
   * The canonical entity key for a relationship — the ordered seed-name pair plus the type, shared
   * with {@code RelationshipSeedApplier} so apply-side resolution derives keys exactly as detection
   * minted them. The pair is stored in seed-file order on both sides (the applier re-derives the
   * key from the row's persisted wrestlers in stored order), so a key always re-derives from its
   * own row.
   */
  static String entityKeyFor(String name1, String name2, RelationshipType type) {
    return "relationship:" + slug(name1) + "<" + type.name() + ">" + slug(name2);
  }

  private static String slug(String name) {
    return name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a relationship row, in fingerprint order: level,
   * isStoryline, notes. The (pair, type) identity is the lookup key. Public for the seed-provenance
   * manifest generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String relationshipFingerprint(WrestlerRelationship relationship) {
    return syncSupport.fingerprint(
        relationship.getLevel() == null ? null : Integer.toString(relationship.getLevel()),
        Boolean.toString(relationship.getIsStoryline()),
        relationship.getNotes());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries.
   */
  private void applySilentSeedFields(
      WrestlerRelationship existing, RelationshipImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "level" -> existing.setLevel(dto.getLevel());
        case "isStoryline" -> existing.setIsStoryline(dto.getIsStoryline());
        case "notes" -> existing.setNotes(dto.getNotes());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update wrestler relationship field '" + field + "'");
      }
    }
  }
}

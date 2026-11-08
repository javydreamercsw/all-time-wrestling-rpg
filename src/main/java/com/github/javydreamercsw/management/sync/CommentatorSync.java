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
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.dto.commentator.CommentatorImportDTO;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(210)
public class CommentatorSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "commentator";

  private final SeedSyncSupport syncSupport;
  private final CommentaryService commentaryService;
  private final CommentatorRepository commentatorRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CommentatorSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CommentaryService commentaryService,
      final CommentatorRepository commentatorRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.commentaryService = commentaryService;
    this.commentatorRepository = commentatorRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && commentaryService.countCommentators() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentators.json");
    if (resource.exists()) {
      log.debug("Loading commentators from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CommentatorImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CommentatorImportDTO dto : dtos) {
          commentaryService.createOrUpdateCommentator(
              dto.getNpcName(),
              dto.getGender(),
              dto.getAlignment(),
              dto.getDescription(),
              dto.getStyle(),
              dto.getCatchphrase(),
              dto.getPersonaDescription(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          // Stamp the freshly seeded row so verify-mode-style checks and the version-change
          // detection pass can match it against the manifest. The service's save leaves the stamp
          // unset; the NPC-side fields are stamped in the npc domain, this stamps the
          // commentator-domain fields.
          commentaryService
              .findCommentatorByNpcName(dto.getNpcName())
              .filter(c -> SeedSyncSupport.isBlankFingerprint(c.getSeedFingerprint()))
              .ifPresent(
                  c -> {
                    c.setSeedFingerprint(commentatorFingerprint(c));
                    commentatorRepository.save(c);
                  });
          log.debug("Loaded commentator: {}", dto.getNpcName());
        }
        log.debug("Commentator loading completed - {} commentators loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentators from file", e);
      }
    } else {
      log.warn("Commentators file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing commentators are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>The NPC-side fields the seed write also touches (description, alignment, expansionCode)
   * belong to the npc domain's stamp ({@code NpcSync}); this domain stamps only the
   * commentator-table surface (style, catchphrase, personaDescription).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || commentaryService.countCommentators() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("commentators.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CommentatorImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Commentator> existingByNpcName =
          commentatorRepository.findAll().stream()
              .filter(c -> c.getNpc() != null)
              .collect(Collectors.toMap(c -> c.getNpc().getName(), c -> c, (a, b) -> a));
      List<Commentator> toSave = new ArrayList<>();
      for (CommentatorImportDTO dto : dtos) {
        Commentator existing = existingByNpcName.get(dto.getNpcName());
        if (existing == null) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getNpcName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(existing, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getNpcName()),
                dto.getNpcName(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::commentatorValueListFingerprint,
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
        commentatorRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (commentator) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during commentator seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one commentator row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Commentator existing, CommentatorImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "style", null, existing.getStyle(), dto.getStyle()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "catchphrase", null, existing.getCatchphrase(), dto.getCatchphrase()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "personaDescription",
            null,
            existing.getPersonaDescription(),
            dto.getPersonaDescription()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(CommentatorImportDTO dto) {
    return syncSupport.fingerprint(
        dto.getStyle(), dto.getCatchphrase(), dto.getPersonaDescription());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #commentatorFingerprint(Commentator)} exactly (see WrestlerSync's raw
   * pass-through shape).
   */
  private String commentatorValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1), values.get(2));
  }

  private static String entityKey(String npcName) {
    return entityKeyFor(npcName);
  }

  /**
   * The canonical entity key for a commentator's NPC name — shared with {@code
   * CommentatorSeedApplier} so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String npcName) {
    return "commentator:" + npcName.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a commentator row, in fingerprint order: style,
   * catchphrase, personaDescription. The NPC-side fields the seed write also touches live in the
   * npc domain's stamp. Public for the seed-provenance manifest generator, which re-hashes fresh
   * seed rows through the owning sync's own method.
   */
  public String commentatorFingerprint(Commentator commentator) {
    return syncSupport.fingerprint(
        commentator.getStyle(), commentator.getCatchphrase(), commentator.getPersonaDescription());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries.
   */
  private void applySilentSeedFields(
      Commentator existing, CommentatorImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "style" -> existing.setStyle(dto.getStyle());
        case "catchphrase" -> existing.setCatchphrase(dto.getCatchphrase());
        case "personaDescription" -> existing.setPersonaDescription(dto.getPersonaDescription());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update commentator field '" + field + "'");
      }
    }
  }
}

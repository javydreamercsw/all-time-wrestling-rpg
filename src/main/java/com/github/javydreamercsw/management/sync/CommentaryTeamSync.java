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
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.dto.commentator.CommentaryTeamImportDTO;
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
@Order(220)
public class CommentaryTeamSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "commentary_team";

  private final SeedSyncSupport syncSupport;
  private final CommentaryService commentaryService;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CommentaryTeamSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CommentaryService commentaryService,
      final CommentaryTeamRepository commentaryTeamRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.commentaryService = commentaryService;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && commentaryService.countTeams() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentary_teams.json");
    if (resource.exists()) {
      log.debug("Loading commentary teams from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CommentaryTeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CommentaryTeamImportDTO dto : dtos) {
          commentaryService.createOrUpdateTeam(dto.getTeamName(), dto.getMemberNames());
          // Stamp the freshly seeded row so verify-mode-style checks and the version-change
          // detection pass can match it against the manifest. The service's save leaves the stamp
          // unset; the team's only seed-owned content is its membership, so stamp after the
          // members resolve.
          commentaryService
              .findTeamByName(dto.getTeamName())
              .filter(t -> SeedSyncSupport.isBlankFingerprint(t.getSeedFingerprint()))
              .ifPresent(
                  t -> {
                    t.setSeedFingerprint(teamFingerprint(t));
                    commentaryTeamRepository.save(t);
                  });
          log.debug("Loaded commentary team: {}", dto.getTeamName());
        }
        log.debug("Commentary team loading completed - {} teams loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentary teams from file", e);
      }
    } else {
      log.warn("Commentary teams file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing commentary teams are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>The team's only seed-owned content is its membership ({@code members}, canonicalized as the
   * sorted display names of the persisted {@code commentators} join — the M2M list has no order
   * guarantee across restarts); the name is the lookup key.
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || commentaryService.countTeams() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("commentary_teams.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CommentaryTeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CommentaryTeam> existingByName =
          commentaryTeamRepository.findAll().stream()
              .collect(Collectors.toMap(CommentaryTeam::getName, t -> t, (a, b) -> a));
      List<CommentaryTeam> toSave = new ArrayList<>();
      for (CommentaryTeamImportDTO dto : dtos) {
        CommentaryTeam existing = existingByName.get(dto.getTeamName());
        if (existing == null) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getTeamName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(existing, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getTeamName()),
                dto.getTeamName(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::teamValueListFingerprint,
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
        commentaryTeamRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (commentary team) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during commentary team seed-review detection", e);
    }
  }

  /**
   * The detection-time per-field observations for one commentary team row against its seed DTO. The
   * persisted membership is compared in canonical (sorted) form — the M2M join list has no order
   * guarantee across restarts, so both sides sort before hashing.
   */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      CommentaryTeam existing, CommentaryTeamImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "members", null, membersCanonical(existing.getCommentators()), membersCanonical(dto)));
    return obs;
  }

  /**
   * Canonical membership serialization: the member NPCs' names sorted, newline-joined (the seed
   * file's order is arbitrary and the persisted M2M list has no order guarantee across reloads).
   * The CommentaryTeamImportDTO overload resolves member names directly — the seed write only
   * stores resolvable members, so unresolved seed names never reach the row and are excluded here
   * exactly like {@link CommentaryService#createOrUpdateTeam}.
   */
  private static String membersCanonical(CommentaryTeamImportDTO dto) {
    return dto.getMemberNames() == null
        ? ""
        : dto.getMemberNames().stream().sorted().collect(Collectors.joining("\n"));
  }

  private static String membersCanonical(List<Commentator> commentators) {
    return commentators == null
        ? ""
        : commentators.stream()
            .filter(c -> c.getNpc() != null)
            .map(c -> c.getNpc().getName())
            .sorted()
            .collect(Collectors.joining("\n"));
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(CommentaryTeamImportDTO dto) {
    return syncSupport.fingerprint(membersCanonical(dto));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #teamFingerprint(CommentaryTeam)} exactly (see WrestlerSync's raw pass-through
   * shape).
   */
  private String teamValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a commentary team name — shared with {@code
   * CommentaryTeamSeedApplier} so apply-side resolution derives keys exactly as detection minted
   * them.
   */
  static String entityKeyFor(String name) {
    return "commentary_team:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a commentary team row, in fingerprint order: members
   * (sorted, newline-joined). The name is the lookup key. Public for the seed-provenance manifest
   * generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String teamFingerprint(CommentaryTeam team) {
    return syncSupport.fingerprint(membersCanonical(team.getCommentators()));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the
   * membership write goes through the service so member resolution stays in one place.
   */
  private void applySilentSeedFields(
      CommentaryTeam existing, CommentaryTeamImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "members" ->
            commentaryService.createOrUpdateTeam(dto.getTeamName(), dto.getMemberNames());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update commentary team field '" + field + "'");
      }
    }
  }
}

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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.dto.TournamentDefinitionDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Seeds the predefined tournament catalog from tournaments.json (ATW-vg16), mirroring the
 * SegmentTypeSync pattern. Runs after SegmentRuleSync (@Order 30) so the referenced rules exist.
 * Upserts by {@code code} — lifecycle state (status, entries, rounds) is never touched on re-sync,
 * so a consumed or completed seeded tournament stays consumed.
 */
@Slf4j
@Component
@Order(55)
public class TournamentSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "tournament";

  private final SeedSyncSupport syncSupport;
  private final TournamentService tournamentService;
  private final TournamentRepository tournamentRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public TournamentSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final TournamentService tournamentService,
      final TournamentRepository tournamentRepository,
      final SegmentRuleRepository segmentRuleRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.tournamentService = tournamentService;
    this.tournamentRepository = tournamentRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && tournamentService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("tournaments.json");
    if (resource.exists()) {
      log.debug("Loading tournament definitions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<TournamentDefinitionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (TournamentDefinitionDTO dto : dtos) {
          Tournament tournament =
              tournamentService.createOrUpdateTournament(
                  dto.code(),
                  dto.name(),
                  dto.formatId(),
                  dto.defaultEntrantCount(),
                  dto.allowedRules());
          if (SeedSyncSupport.isBlankFingerprint(tournament.getSeedFingerprint())) {
            // Stamp the freshly seeded row so verify mode's stored-side check and the
            // version-change detection pass can match it against the manifest. Lifecycle state
            // (status, entries, rounds) stays untouched — only the seed-owned fields are hashed.
            tournament.setSeedFingerprint(tournamentFingerprint(tournament));
            tournamentService.save(tournament);
          }
          log.debug("Loaded tournament: {} ({})", tournament.getName(), tournament.getCode());
        }
        log.debug("Tournament catalog loading completed");
      } catch (IOException e) {
        log.error("Error loading tournaments.json", e);
      }
    } else {
      log.warn("Tournament catalog file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing catalog tournaments are classified
   * field-by-field through {@link SeedCollisionDetector} instead of being reconciled directly —
   * tier-1 fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3
   * fields stay untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load
   * already applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || tournamentService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("tournaments.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<TournamentDefinitionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      List<Tournament> toSave = new ArrayList<>();
      for (TournamentDefinitionDTO dto : dtos) {
        // Catalog rows resolve by code — the seed's stable identity. The name is seed-owned
        // content (a collision target), so it can never serve as the resolution handle (same
        // contract as the card domain's set#number key).
        Tournament tournament = tournamentRepository.findByCodeWithRules(dto.code()).orElse(null);
        if (tournament == null) {
          continue; // booker-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.code(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(tournament.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(tournament, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.code()),
                dto.name(),
                rowHasStoredStamp,
                tournament.getSeedFingerprint(),
                observations,
                this::tournamentValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          // The write is field-scoped in place; lifecycle state (status, entries, rounds) is never
          // touched and a bracket-locked format is never changed (the service's lock rule).
          applySilentSeedFields(tournament, dto, result.silentUpdateFields());
          toSave.add(tournament);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(tournament.getSeedFingerprint())) {
          tournament.setSeedFingerprint(stamp);
          if (!toSave.contains(tournament)) {
            toSave.add(tournament);
          }
        }
      }
      if (!toSave.isEmpty()) {
        for (Tournament tournament : toSave) {
          tournamentService.save(tournament);
        }
      }
      log.info(
          "Seed review detection (tournament) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during tournament seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one catalog row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Tournament tournament, TournamentDefinitionDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation("name", null, tournament.getName(), dto.name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "formatId",
            null,
            // A bracket locks the format once entries exist; the observation reports the row's
            // live value either way and the tier-1 writer enforces the lock.
            tournament.getFormatId(),
            dto.formatId()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "defaultEntrantCount",
            null,
            tournament.getDefaultEntrantCount() == null
                ? null
                : tournament.getDefaultEntrantCount().toString(),
            dto.defaultEntrantCount() == null ? null : dto.defaultEntrantCount().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "allowedRules",
            null,
            rulesCanonical(resolvedNames(tournament.getAllowedRules())),
            rulesCanonical(resolveRuleNames(dto.allowedRules()))));
    return obs;
  }

  /**
   * Canonical rules-pool serialization: names sorted (the persisted {@code @ManyToMany} list has no
   * order guarantee across restarts, so both sides sort before hashing).
   */
  private String rulesCanonical(List<String> names) {
    return names.stream().sorted().collect(Collectors.joining("\n"));
  }

  /** The names of the given resolved rules. */
  private List<String> resolvedNames(List<SegmentRule> rules) {
    return rules.stream().map(SegmentRule::getName).collect(Collectors.toList());
  }

  /**
   * Resolves seed rule names to their rules the way {@code createOrUpdateTournament} does — unknown
   * names are skipped (mirroring the service's seed tolerance), so the fingerprint matches what the
   * seed write would leave on the row.
   */
  private List<String> resolveRuleNames(List<String> allowedRuleNames) {
    List<String> resolved = new ArrayList<>();
    for (String name : allowedRuleNames) {
      // Unknown seed names are skipped (the service's seed tolerance) — ifPresent adds nothing.
      segmentRuleRepository.findByName(name).ifPresent(rule -> resolved.add(rule.getName()));
    }
    return resolved;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(TournamentDefinitionDTO dto) {
    return syncSupport.fingerprint(
        dto.name(),
        dto.formatId(),
        dto.defaultEntrantCount(),
        rulesCanonical(resolveRuleNames(dto.allowedRules())));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #tournamentFingerprint(Tournament)} exactly.
   */
  private String tournamentValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so the observed string
    // forms hash identically to the row fingerprint's typed values (see WrestlerSync's shape).
    return syncSupport.fingerprint(values.toArray());
  }

  private static String entityKey(String code) {
    return entityKeyFor(code);
  }

  /**
   * The canonical entity key for a tournament code — shared with {@code TournamentSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String code) {
    return "tournament:" + code.toLowerCase();
  }

  /**
   * Repository access for the manifest generator — it re-hashes every stamped catalog row through
   * {@link #tournamentFingerprint} and needs the eager-rules query ({@code findAllWithRules}),
   * which lives on the repository rather than the service.
   */
  public com.github.javydreamercsw.management.domain.tournament.TournamentRepository
      repositoryForManifest() {
    return tournamentRepository;
  }

  /**
   * Canonical seed-owned field values for a catalog tournament row, in fingerprint order: name,
   * formatId, defaultEntrantCount, allowedRules (names sorted, newline-joined). Lifecycle state
   * (status, entries, rounds, universe) is excluded — the catalog write never touches it. Public
   * for the seed-provenance manifest generator, which re-hashes fresh seed rows through the owning
   * sync's own method.
   */
  public String tournamentFingerprint(Tournament tournament) {
    return syncSupport.fingerprint(
        tournament.getName(),
        tournament.getFormatId(),
        tournament.getDefaultEntrantCount(),
        rulesCanonical(resolvedNames(tournament.getAllowedRules())));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the write
   * semantics mirror {@code createOrUpdateTournament} (rules resolve by name, unknown names
   * skipped; the format stays locked once a bracket exists).
   */
  private void applySilentSeedFields(
      Tournament tournament, TournamentDefinitionDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "name" -> tournament.setName(dto.name());
        case "formatId" -> {
          // The format is locked once a bracket exists (the service's createOrUpdateTournament
          // rule) — a running tournament keeps its format; the re-stamp still converges the row.
          if (tournament.getEntries().isEmpty()) {
            tournament.setFormatId(dto.formatId());
          }
        }
        case "defaultEntrantCount" -> tournament.setDefaultEntrantCount(dto.defaultEntrantCount());
        case "allowedRules" ->
            tournament.setAllowedRules(new ArrayList<>(resolveRuleEntities(dto.allowedRules())));
        default ->
            throw new IllegalStateException(
                "Unknown silent-update tournament field '" + field + "'");
      }
    }
  }

  /** Resolves seed rule names to rule entities for the tier-1 write (unknown names skipped). */
  private List<SegmentRule> resolveRuleEntities(List<String> allowedRuleNames) {
    List<SegmentRule> resolved = new ArrayList<>();
    for (String name : allowedRuleNames) {
      segmentRuleRepository.findByName(name).ifPresent(resolved::add);
    }
    return resolved;
  }
}

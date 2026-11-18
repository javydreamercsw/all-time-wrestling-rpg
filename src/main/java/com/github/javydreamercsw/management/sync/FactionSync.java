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
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
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
@Order(160)
public class FactionSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "faction";

  private final SeedSyncSupport syncSupport;
  private final FactionService factionService;
  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final UniverseRepository universeRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public FactionSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final FactionService factionService,
      final FactionRepository factionRepository,
      final WrestlerRepository wrestlerRepository,
      final NpcService npcService,
      final UniverseRepository universeRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.factionService = factionService;
    this.factionRepository = factionRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.universeRepository = universeRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && factionService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("factions.json");
    if (resource.exists()) {
      log.debug("Loading factions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<FactionImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Long universeId =
            universeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No universe found"))
                .getId();
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAllUnfiltered().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        for (FactionImportDTO dto : dtos) {
          Optional<Wrestler> leaderOpt = Optional.ofNullable(wrestlersByName.get(dto.getLeader()));
          if (leaderOpt.isPresent()) {
            Optional<Faction> factionOpt = factionService.getFactionByName(dto.getName());
            if (factionOpt.isEmpty()) {
              factionOpt =
                  factionService.createFaction(
                      dto.getName(), dto.getDescription(), leaderOpt.get().getId(), universeId);
            }
            if (factionOpt.isPresent()) {
              Faction faction = factionOpt.get();
              for (String memberName : dto.getMembers()) {
                Optional.ofNullable(wrestlersByName.get(memberName))
                    .ifPresent(
                        wrestler ->
                            factionService.addMemberToFaction(faction.getId(), wrestler.getId()));
              }
              if (dto.getManager() != null) {
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  faction.setManager(manager);
                  factionService.save(faction);
                }
              }
              // Stamp the freshly seeded row so verify-mode-style checks and the version-change
              // detection pass can match it against the manifest. The seed write may not have
              // changed anything (create path saves before manager/members resolve), so only a
              // blank stamp is written — an already-stamped row keeps its stamp.
              if (SeedSyncSupport.isBlankFingerprint(faction.getSeedFingerprint())) {
                faction.setSeedFingerprint(factionFingerprint(faction));
                factionService.save(faction);
              }
            }
          }
          log.debug("Loaded faction: {}", dto.getName());
        }
        log.debug("Faction loading completed - {} factions processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading factions from file", e);
      }
    } else {
      log.warn("Factions file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing factions are classified field-by-field through
   * {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields update
   * silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay untouched.
   * Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already applies the seed
   * everywhere; an empty DB has nothing to collide).
   *
   * <p>Faction membership is gameplay state tracked on the member rows ({@code
   * WrestlerState.faction}) — the seed-owned reviewable surface is the row's own fields (name is
   * the lookup key, description/manager/affinity the seed write set).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || factionService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("factions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<FactionImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Faction> existingByName =
          factionRepository.findAllWithManager().stream()
              .collect(Collectors.toMap(Faction::getName, f -> f, (a, b) -> a));
      List<Faction> toSave = new ArrayList<>();
      for (FactionImportDTO dto : dtos) {
        Faction existing = existingByName.get(dto.getName());
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
                this::factionValueListFingerprint,
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
        for (Faction faction : toSave) {
          factionService.save(faction);
        }
      }
      log.info(
          "Seed review detection (faction) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during faction seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one faction row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Faction existing, FactionImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "managerName",
            null,
            existing.getManager() == null ? null : existing.getManager().getName(),
            dto.getManager()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "affinity",
            null,
            Integer.toString(existing.getAffinity()),
            Integer.toString(dto.getAffinity())));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(FactionImportDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(), dto.getManager(), Integer.toString(dto.getAffinity()));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #factionFingerprint(Faction)} exactly: the observed strings hash identically to
   * the typed values (see WrestlerSync's raw pass-through shape).
   */
  private String factionValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1), values.get(2));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a faction name — shared with {@code FactionSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "faction:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a faction row, in fingerprint order: description,
   * managerName, affinity. Membership (leader, member list) is gameplay state on the member rows
   * and excluded; {@code active} and the dates are lifecycle state the seed write does not own.
   * Public for the seed-provenance manifest generator, which re-hashes fresh seed rows through the
   * owning sync's own method.
   */
  public String factionFingerprint(Faction faction) {
    return syncSupport.fingerprint(
        faction.getDescription(),
        faction.getManager() == null ? null : faction.getManager().getName(),
        Integer.toString(faction.getAffinity()));
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the
   * manager reference resolves by name through the same NPC lookup the seed write uses (unknown
   * names log and skip, mirroring the FULL path's tolerance).
   */
  private void applySilentSeedFields(
      Faction existing, FactionImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "managerName" -> {
          if (dto.getManager() != null) {
            npcService.findAllUnfiltered().stream()
                .filter(npc -> npc.getName().equals(dto.getManager()))
                .findFirst()
                .ifPresentOrElse(
                    existing::setManager,
                    () ->
                        log.warn(
                            "NPC manager '{}' not found — manager reference not changed on"
                                + " faction '{}'",
                            dto.getManager(),
                            dto.getName()));
          } else {
            existing.setManager(null);
          }
        }
        case "affinity" -> existing.setAffinity(dto.getAffinity());
        default ->
            throw new IllegalStateException("Unknown silent-update faction field '" + field + "'");
      }
    }
  }
}

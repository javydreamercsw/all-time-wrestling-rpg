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
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Order(170)
public class TeamSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "team";

  private final SeedSyncSupport syncSupport;
  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final ObjectMapper objectMapper;
  private final UniverseRepository universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public TeamSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final TeamService teamService,
      final TeamRepository teamRepository,
      final WrestlerRepository wrestlerRepository,
      final NpcService npcService,
      final ObjectMapper objectMapper,
      final UniverseRepository universeRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && teamService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("teams.json");
    if (resource.exists()) {
      log.debug("Loading teams from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<TeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAllUnfiltered().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        List<Universe> universes = universeRepository.findAll();
        for (TeamImportDTO dto : dtos) {
          List<Wrestler> members = resolveMembers(dto, wrestlersByName);
          if (members.size() == expectedMemberCount(dto)) {
            Optional<Team> teamOpt = teamService.getTeamByName(dto.getName());
            if (teamOpt.isEmpty()) {
              teamOpt =
                  teamService.createTeam(
                      dto.getName(),
                      dto.getDescription(),
                      members.stream().map(Wrestler::getId).toList(),
                      teamSizeOf(dto),
                      null,
                      null,
                      TeamOrigin.REGISTRY);
            }
            if (teamOpt.isPresent()) {
              Team team = teamOpt.get();
              if (dto.getManager() != null) {
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  team.setManager(manager);
                  teamRepository.save(team);
                }
              }
              // Scope the seeded team to its members' shared universe (ATW-01g2 review):
              // tournament eligibility pools the registry through team.universe, so a
              // universe-less seed never reaches the wizard's registry grid. Runs after
              // WrestlerSync (@Order 120), so member states exist to agree on.
              assignUniverseFromMembers(team, universes);
              // Stamp the freshly seeded row so verify-mode-style checks and the version-change
              // detection pass can match it against the manifest. The universe/manager writes
              // above may have re-saved the row already; only a blank stamp is written — an
              // already-stamped row keeps its stamp.
              if (SeedSyncSupport.isBlankFingerprint(team.getSeedFingerprint())) {
                team.setSeedFingerprint(teamFingerprint(team));
                teamRepository.save(team);
              }
            }
          } else {
            log.warn(
                "Skipping team '{}': expected {} member(s) resolvable by name, found {}",
                dto.getName(),
                expectedMemberCount(dto),
                members.size());
          }
          log.debug("Loaded team: {}", dto.getName());
        }
        log.debug("Team loading completed - {} teams processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading teams from file", e);
      }
    } else {
      log.warn("Teams file not found: {}", resource.getPath());
    }
  }

  /**
   * Scopes {@code team} to the universe all its members agree on: the universe where EVERY member
   * holds a wrestler_state. Ambiguous (members span universes) or stateless memberships leave the
   * team unassigned with a warning — the eligibility pool treats unassigned teams as never scoped,
   * never silently dropped into a wrong universe. A team already carrying a universe (e.g. resolved
   * from the session context at create) is left untouched.
   *
   * @param team the freshly seeded team
   * @param universes every universe in the install (seed installs hold one or a handful)
   */
  private void assignUniverseFromMembers(final Team team, final List<Universe> universes) {
    if (team.getUniverse() != null) {
      return;
    }
    Set<Long> shared = null;
    for (TeamMember member : team.getMembers()) {
      Set<Long> memberUniverseIds = new HashSet<>();
      for (Universe universe : universes) {
        if (wrestlerStateRepository
            .findByWrestlerAndUniverse(member.getWrestler(), universe)
            .isPresent()) {
          memberUniverseIds.add(universe.getId());
        }
      }
      if (shared == null) {
        shared = new HashSet<>(memberUniverseIds);
      } else {
        shared.retainAll(memberUniverseIds);
      }
    }
    if (shared == null || shared.isEmpty()) {
      log.warn(
          "Team '{}' members hold no wrestler states — team.universe left unset", team.getName());
      return;
    }
    if (shared.size() > 1) {
      log.warn(
          "Team '{}' members span {} universes — team.universe left unset (ambiguous)",
          team.getName(),
          shared.size());
      return;
    }
    universeRepository
        .findById(shared.iterator().next())
        .ifPresent(
            universe -> {
              team.setUniverse(universe);
              teamRepository.save(team);
              log.debug(
                  "Scoped seeded team '{}' to universe '{}' (member agreement)",
                  team.getName(),
                  universe.getName());
            });
  }

  /**
   * Resolves the entry's member list: the ordered {@code members} array when present, otherwise the
   * legacy {@code wrestler1}/{@code wrestler2} pair.
   *
   * @param dto the catalog entry
   * @param wrestlersByName wrestlers indexed by name
   * @return the resolved members, in slot order (missing names are skipped)
   */
  private List<Wrestler> resolveMembers(
      final TeamImportDTO dto, final Map<String, Wrestler> wrestlersByName) {
    List<String> names =
        dto.hasLegacyWrestlerFields()
            ? List.of(dto.getWrestler1(), dto.getWrestler2())
            : dto.getMembers();
    if (names == null) {
      return List.of();
    }
    return names.stream()
        .map(wrestlersByName::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing catalog teams are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>Team membership is gameplay state on the member rows ({@code TeamMember}) and the manager/
   * universe references resolve from it — the seed-owned reviewable surface is the row's own fields
   * (name is the lookup key, description/teamSize the seed write set).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || teamService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("teams.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<TeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      List<Team> toSave = new ArrayList<>();
      for (TeamImportDTO dto : dtos) {
        Team existing = teamRepository.findByName(dto.getName()).orElse(null);
        if (existing == null) {
          continue; // booker-created rows have no seed match; missing rows are the FULL load's job
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
        // Direct repository save: the detection pass only needs the tier-1 field updates and the
        // stamp persisted — the service's updateTeam path carries booker-flow validation and
        // history writes that must not run at seed time.
        teamRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (team) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during team seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one team row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Team existing, TeamImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "teamSize",
            null,
            existing.getTeamSize() == null ? null : existing.getTeamSize().name(),
            teamSizeOf(dto).name()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(TeamImportDTO dto) {
    return syncSupport.fingerprint(dto.getDescription(), teamSizeOf(dto).name());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #teamFingerprint(Team)} exactly: the observed strings hash identically to the
   * typed values (see WrestlerSync's raw pass-through shape).
   */
  private String teamValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a team name — shared with {@code TeamSeedApplier} so apply-side
   * resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "team:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a team row, in fingerprint order: description, teamSize.
   * Membership, manager, universe, status, fans and the dates are gameplay/lifecycle state the
   * catalog write does not own. Public for the seed-provenance manifest generator, which re-hashes
   * fresh seed rows through the owning sync's own method.
   */
  public String teamFingerprint(Team team) {
    return syncSupport.fingerprint(
        team.getDescription(), team.getTeamSize() == null ? null : team.getTeamSize().name());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries. The
   * declared size re-derives from the member list on the next persist ({@code
   * Team.ensureDefaults}), so an applied size that disagrees with the member count converges there
   * rather than corrupting the row.
   */
  private void applySilentSeedFields(Team existing, TeamImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "teamSize" -> existing.setTeamSize(teamSizeOf(dto));
        default ->
            throw new IllegalStateException("Unknown silent-update team field '" + field + "'");
      }
    }
  }

  /**
   * Parses the entry's declared team size ({@link TeamSize#DUO} default for back-compat).
   *
   * @throws IllegalArgumentException when the code is not a {@link TeamSize} name
   */
  private TeamSize teamSizeOf(final TeamImportDTO dto) {
    return TeamSize.valueOf(dto.getTeamSize());
  }

  /** How many members the entry must resolve for the sync to create it. */
  private int expectedMemberCount(final TeamImportDTO dto) {
    return teamSizeOf(dto).getMemberCount();
  }
}

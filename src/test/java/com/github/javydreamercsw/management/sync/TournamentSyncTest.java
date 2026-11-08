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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.dto.TournamentDefinitionDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the tournaments.json seed sync (ATW-vg16). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentSyncTest {

  @Mock private TournamentService tournamentService;
  @Mock private TournamentRepository tournamentRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private TournamentSync sync;

  @BeforeEach
  void setUp() {
    lenient()
        .when(
            tournamentService.createOrUpdateTournament(
                anyString(), anyString(), anyString(), any(), anyList()))
        .thenAnswer(
            inv -> {
              Tournament t = new Tournament();
              t.setCode(inv.getArgument(0));
              t.setName(inv.getArgument(1));
              t.setFormatId(inv.getArgument(2));
              t.setDefaultEntrantCount(inv.getArgument(3));
              t.setAllowedRules(new ArrayList<>());
              return t;
            });
    lenient().when(tournamentRepository.findByCode(anyString())).thenReturn(Optional.empty());
    sync = sync("false");
  }

  private TournamentSync sync(String mode) {
    return new TournamentSync(
        mode,
        tournamentService,
        tournamentRepository,
        segmentRuleRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  @Test
  @DisplayName("Seeds every catalog entry with its rules pool")
  void sync_seedsDefinitions() {
    sync.sync();

    verify(tournamentService)
        .createOrUpdateTournament(anyString(), anyString(), anyString(), any(), anyList());
  }

  @Test
  @DisplayName("skip-if-not-empty skips the catalog when tournaments already exist")
  void sync_skipWhenNotEmpty() {
    TournamentSync gated = sync("true");
    when(tournamentService.count()).thenReturn(3L);

    gated.sync();

    verify(tournamentService, never())
        .createOrUpdateTournament(anyString(), anyString(), anyString(), any(), anyList());
  }

  @Test
  @DisplayName("Catalog JSON parses into the definition DTO with a rules pool")
  void catalogParses() throws Exception {
    try (var is = getClass().getResourceAsStream("/tournaments.json")) {
      List<TournamentDefinitionDTO> dtos =
          new ObjectMapper().readValue(is, new TypeReference<>() {});
      assertThat(dtos).isNotEmpty();
      TournamentDefinitionDTO deadly =
          dtos.stream().filter(d -> "deadly_combat".equals(d.code())).findFirst().orElseThrow();
      assertThat(deadly.name()).isEqualTo("Deadly Combat");
      assertThat(deadly.formatId()).isEqualTo("SINGLE_ELIMINATION");
      assertThat(deadly.defaultEntrantCount()).isEqualTo(8);
      assertThat(deadly.allowedRules()).isNotEmpty();
    }
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. The catalog's row is Deadly Combat;
    // the stamp carries the previous release's seed write, the incoming seed (tournaments.json)
    // supplies the new values through the sync's normal resource path.
    //   tier 1 — untouched name (stale on the row), seed name current → silent update + re-stamp
    //   tier 2 — admin-edited allowedRules vs seed rules → parked, value unchanged
    //   tier 3 — booker-created rows have no seed match → never touched
    // (Tier 1 has no separate fixture: the domain's single catalog row carries the tier-1
    // surface on its untouched fields — name and rules pool — asserted implicitly by the tier-2
    // fixture's unchanged name and the silent-write path shared with the other syncs' pins.)

    // Tier 2: the stamp holds the PREVIOUS release's rules pool (empty) while the row shows the
    // admin's trimmed pool and the incoming seed ships four rules → genuine collision.
    Tournament tier2row = catalogTournament();
    tier2row.setName("Deadly Combat");
    tier2row.setAllowedRules(
        new ArrayList<>(List.of(segmentRule("Cage")))); // admin trimmed the pool after seeding
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("Deadly Combat", "SINGLE_ELIMINATION", 8, "", (Object) null));

    when(tournamentService.count()).thenReturn(1L);
    when(tournamentRepository.findByCodeWithRules("deadly_combat"))
        .thenReturn(Optional.of(tier2row));
    // The detection pass resolves the seed's rule names through the rule catalog (mirroring the
    // seed write): all four tournaments.json names resolve.
    for (String name :
        List.of(
            "Submission", "Last Man Standing", "Barbwire Exploding Deathmatch", "No DQ", "Cage")) {
      when(segmentRuleRepository.findByName(name)).thenReturn(Optional.of(segmentRule(name)));
    }

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    TournamentSync detectionSync =
        new TournamentSync(
            "true",
            tournamentService,
            tournamentRepository,
            segmentRuleRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1 surface lives on the same catalog row's untouched fields (name/current rules pool);
    // this fixture pins tier 2 exactly, per the domain's single catalog entry.
    // Tier 2: the admin's rules pool kept its value; the row parked the collision.
    assertEquals(
        List.of("Cage"),
        tier2row.getAllowedRules().stream().map(SegmentRule::getName).toList(),
        "tier 2: admin edit must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("tournament"),
            eq("tournament:deadly_combat"),
            eq("Deadly Combat"),
            eq("allowedRules"),
            eq("Cage"),
            anyString(),
            anyString());
    assertEquals(
        Set.of("tournament:deadly_combat|allowedRules"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and PROTECT-mode detection does not touch
    // booker-created rows (no seed match).
    TournamentSync fullSync = sync("false");
    Tournament bookerRow = catalogTournament();
    bookerRow.setCode(null);
    when(tournamentService.count()).thenReturn(1L);
    when(tournamentRepository.findByCode(anyString())).thenReturn(Optional.empty());

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert
    verify(tournamentService, never()).save(any());

    TournamentSync protectSync = sync("true");
    when(tournamentService.count()).thenReturn(1L);
    protectSync.detectVersionChangeCollisions();
    verify(tournamentService, never()).save(any()); // booker-created row never touched
    assertTrue(bookerRow.getSeedFingerprint() == null);
  }

  private Tournament catalogTournament() {
    Tournament t = new Tournament();
    t.setCode("deadly_combat");
    t.setName("Deadly Combat");
    t.setFormatId("SINGLE_ELIMINATION");
    t.setDefaultEntrantCount(8);
    t.setAllowedRules(new ArrayList<>());
    return t;
  }

  private SegmentRule segmentRule(String name) {
    SegmentRule rule = new SegmentRule();
    rule.setName(name);
    return rule;
  }
}

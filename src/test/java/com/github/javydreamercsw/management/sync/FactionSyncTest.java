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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the factions.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FactionSyncTest {

  @Mock private FactionService factionService;

  @Mock
  private com.github.javydreamercsw.management.domain.faction.FactionRepository factionRepository;

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcService npcService;
  @Mock private UniverseRepository universeRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private FactionSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(factionService.count()).thenReturn(0L);
  }

  private FactionSync sync(String mode) {
    return new FactionSync(
        mode,
        factionService,
        factionRepository,
        wrestlerRepository,
        npcService,
        universeRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static Faction faction(String name, String description) {
    return Faction.builder().name(name).description(description).build();
  }

  private static Npc managerNpc(String name) {
    Npc n = new Npc();
    n.setName(name);
    n.setDescription("manager description");
    n.setNpcType("Manager");
    return n;
  }

  private static Wrestler wrestler(String name) {
    return Wrestler.builder().name(name).build();
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. The factions.json seed entries carry
    // no description and no affinity, so the incoming seed values are null and 0 respectively.
    // "Team USA" is the tier-1 row (description stale vs the stamp, seed current); "The Hurt
    // Business" the tier-2-shaped row — an admin affinity edit the seed did NOT change, which the
    // single-edit reconstruction must classify tier 3 (preserve), parking nothing.
    Faction tier1row = faction("Team USA", "stale description");
    tier1row.setAffinity(2);
    tier1row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("stale description", null, "2"));

    Faction tier2row = faction("The Hurt Business", null);
    Npc mvp = managerNpc("MVP");
    tier2row.setManager(mvp); // the seed's manager write
    // The stamp carries the PREVIOUS release's seed values (null description, MVP, affinity 0)
    // while the row shows the admin's affinity edit (7) — substituting the incoming seed's 0 for
    // that one field reproduces the stamp → single-edit reconstruction, all other fields attested.
    tier2row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint(null, "MVP", "0"));
    tier2row.setAffinity(7); // admin edit AFTER the stamp → dirty row

    when(factionService.count()).thenReturn(2L);
    when(factionRepository.findAllWithManager()).thenReturn(List.of(tier1row, tier2row));
    when(npcService.findAllUnfiltered()).thenReturn(List.of(mvp));

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    FactionSync detectionSync =
        new FactionSync(
            "true",
            factionService,
            factionRepository,
            wrestlerRepository,
            npcService,
            universeRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value (the seed ships no
    // description → null).
    assertNull(tier1row.getDescription(), "tier 1: seed's null description must apply silently");
    // Tier 2 row: the admin's affinity edit kept its value (seed unchanged → tier 3 preserve).
    assertEquals(7, tier2row.getAffinity(), "tier 3: the seed-unchanged admin edit must persist");
    assertEquals(
        java.util.Set.of(),
        parkedCoordinates,
        "single-edit rows with an unchanged seed park nothing; every field is tier 1 or tier 3");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    FactionSync fullSync = sync("false");
    when(factionService.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(factionRepository, never()).findAllWithManager();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row. The seed write goes through the service
    // (createFaction + addMember + manager save); the stamp write hits only rows the seed left
    // blank-stamped.
    Universe universe = Universe.builder().name("Default Universe").build();
    Wrestler daemon = wrestler("Daemon");
    Faction created = Faction.builder().name("Desolation's Smile").description(null).build();
    when(factionService.count()).thenReturn(0L);
    when(universeRepository.findAll()).thenReturn(List.of(universe));
    when(wrestlerRepository.findAll())
        .thenReturn(List.of(daemon, wrestler("Kamala"), wrestler("OMZ")));
    when(npcService.findAllUnfiltered()).thenReturn(List.of());
    when(factionService.getFactionByName(anyString())).thenReturn(Optional.empty());
    when(factionService.createFaction(anyString(), any(), any(), any()))
        .thenReturn(Optional.of(created));
    when(factionService.addMemberToFaction(any(), any())).thenReturn(Optional.of(created));
    when(factionService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    sync.sync();

    verify(factionService, atLeastOnce())
        .save(ArgumentMatchers.argThat(f -> f.getSeedFingerprint() != null));
  }
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NpcSyncTest {

  @Mock private NpcService npcService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private NpcSync sync;

  private NpcSync sync(String mode) {
    return new NpcSync(mode, npcService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  @BeforeEach
  void setUp() {
    sync = sync("false");
  }

  @Test
  void sync_savesNewNpcs() {
    when(npcService.findAllUnfiltered()).thenReturn(List.of());

    sync.sync();

    verify(npcService, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyFlagSet() {
    // Skip gate with a complete catalog: every seeded NPC exists → top-up inserts nothing.
    NpcSync skipSync = sync("true");
    when(npcService.count()).thenReturn(15L);
    when(npcService.findAllUnfiltered())
        .thenReturn(
            List.of(
                    "Colonel Mustafa",
                    "Charles",
                    "Samuel Winters",
                    "Rezan the Resolved",
                    "Rigo Del Sarto",
                    "Anika Stroud",
                    "Tomás “Kid Whistle” Iglesias",
                    "Beatrix “Trixie” Halvorsen",
                    "Brother Cassian",
                    "Dr. Yumi Sato",
                    "Reginald Fairfax III",
                    "Amara Diallo",
                    "MVP",
                    "Commissioner Blake",
                    "Lena Voss")
                .stream()
                .map(
                    name -> {
                      Npc npc = new Npc();
                      npc.setName(name);
                      return npc;
                    })
                .toList());

    skipSync.sync();

    verify(npcService, never()).saveAll(any());
  }

  @Test
  void sync_doesNotSaveExistingUnchangedNpc() {
    // Charles is in npcs.json with no awareness field — unchanged NPC must not be re-saved
    Npc existing = new Npc();
    existing.setName("Charles");
    existing.setDescription("The mysterious owner of ATW.");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    sync.sync();

    verify(npcService, never())
        .setAwareness(argThat(npc -> "Charles".equals(npc.getName())), anyInt());
  }

  @Test
  void sync_savesExistingNpcWhenDescriptionChanged() {
    Npc existing = new Npc();
    existing.setName("Charles"); // Charles is in npcs.json
    existing.setDescription("stale description"); // differs from file → triggers dirty-check save
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    sync.sync();

    // saveAll must have been called (dirty-check path was taken)
    verify(npcService, atLeastOnce()).saveAll(any());
    // the existing object's description was overwritten by the sync with the file value
    assertEquals("The mysterious owner of ATW.", existing.getDescription());
  }

  @Test
  void sync_topUpInsertsOnlyMissingNpcsWhenSkipFlagSet() {
    // Simulates a prod install with the skip flag set: 4 pre-2.12.0 referees exist, the 6 new
    // ones (and the rest of the seed catalog) are missing. Top-up must insert exactly the 11
    // missing seed NPCs and never re-save an existing row.
    NpcSync skipSync = sync("true");
    when(npcService.count()).thenReturn(4L);
    List<String> existingNames =
        List.of("Samuel Winters", "Rezan the Resolved", "Rigo Del Sarto", "Anika Stroud");
    when(npcService.findAllUnfiltered())
        .thenReturn(
            existingNames.stream()
                .map(
                    name -> {
                      Npc npc = new Npc();
                      npc.setName(name);
                      return npc;
                    })
                .toList());

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Npc>> captor = ArgumentCaptor.forClass(List.class);
    verify(npcService).saveAll(captor.capture());
    List<Npc> saved = captor.getValue();
    assertEquals(11, saved.size(), "npcs.json seeds 15 NPCs; 4 exist → 11 must be inserted");
    Set<String> savedNames = saved.stream().map(Npc::getName).collect(Collectors.toSet());
    // 2.12.0's new referees must be among the inserted rows
    assertTrue(savedNames.contains("Tomás “Kid Whistle” Iglesias"));
    assertTrue(savedNames.contains("Amara Diallo"));
    for (String existing : existingNames) {
      assertFalse(savedNames.contains(existing), "existing NPC must not be re-saved: " + existing);
    }
  }

  @Test
  void sync_topUpLeavesExistingNpcUntouchedWhenSkipFlagSet() {
    NpcSync skipSync = sync("true");
    when(npcService.count()).thenReturn(1L);
    Npc existing = new Npc();
    existing.setName("Charles");
    existing.setDescription("stale description");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    skipSync.sync();

    // Charles is in npcs.json but already exists — under the skip gate his row must not be
    // re-saved (data-protection guarantee) and his stale description must survive.
    verify(npcService, never()).saveAll(argThat(list -> list.contains(existing)));
    assertEquals("stale description", existing.getDescription());
  }

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private Npc npcCanonicalHashed(Npc npc) {
    npc.setSeedFingerprint(npcFingerprint(npc));
    return npc;
  }

  private String npcFingerprint(Npc npc) {
    Object awareness = npc.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS);
    return VERIFY_SUPPORT.fingerprint(
        npc.getDescription(),
        npc.getNpcType(),
        npc.getExpansionCode(),
        npc.getAlignment() == null ? null : npc.getAlignment().name(),
        npc.getGender() == null ? null : npc.getGender().name(),
        awareness == null ? null : awareness.toString());
  }

  @Test
  void verifyMode_reconcilesCleanNpcAndPreservesEditedNpc() {
    NpcSync verifySync = sync("verify");
    when(npcService.count()).thenReturn(2L);

    // Clean: Charles as an older release's seed write left him, description since fixed in the
    // seed file ("The mysterious owner of ATW.").
    Npc clean = new Npc();
    clean.setName("Charles");
    clean.setDescription("stale description from an older release");
    npcCanonicalHashed(clean);

    // Edited: stamped at seed write, then hand-tuned.
    Npc edited = new Npc();
    edited.setName("MVP");
    edited.setDescription("user-tuned description");
    npcCanonicalHashed(edited);
    edited.setDescription("user-tuned description (changed after stamping)");

    when(npcService.findAllUnfiltered()).thenReturn(List.of(clean, edited));

    verifySync.sync();

    assertEquals(
        "The mysterious owner of ATW.",
        clean.getDescription(),
        "clean-fingerprint NPC must adopt the seed description");
    assertEquals(
        "user-tuned description (changed after stamping)",
        edited.getDescription(),
        "edited NPC must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyNpcWhenFieldsMatchSeed() {
    NpcSync verifySync = sync("verify");
    when(npcService.count()).thenReturn(1L);

    // Legacy: no stamp, but fields equal the seed (per npcs.json: description "The mysterious
    // owner of ATW.", type "Owner", expansion BASE_GAME, NEUTRAL alignment, no awareness).
    Npc legacy = new Npc();
    legacy.setName("Charles");
    legacy.setDescription("The mysterious owner of ATW.");
    legacy.setNpcType("Owner");
    legacy.setExpansionCode("BASE_GAME");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(legacy));

    verifySync.sync();

    assertEquals(
        npcFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy NPC matching the seed must be adopted (fingerprint stamped)");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  /** Samuel Winters's npcs.json description, verbatim (the tier-2 fixture aligns with it). */
  private static final String SEED_SAMUEL_DESCRIPTION =
      "A veteran referee with an imposing presence, Samuel Winters brings legitimacy and authority"
          + " to every match he officiates. Standing at 6'2\" with an athletic build, his"
          + " clean-shaven head and stern expression command respect. Winters is known for his"
          + " strict adherence to the rules and is not easily fooled by distractions or illegal"
          + " tactics, making him a referee that heels often try to avoid.";

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline:
    //   tier 1 — untouched field (description), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (awareness), seed changed → parked as PENDING decision, DB
    //            value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (npcType edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    // The DB rows are stamped as the PREVIOUS release's seed write left them; the incoming seed
    // (npcs.json, unchanged on disk) supplies the new values through the sync's normal resource
    // path. Charles is the tier-1/tier-3 row, MVP the tier-2 row.
    Npc tier1row = new Npc();
    tier1row.setName("Charles");
    tier1row.setDescription("stale description from an older release"); // seed changed since
    tier1row.setNpcType("Owner"); // seed value
    tier1row.setExpansionCode("BASE_GAME");
    npcCanonicalHashed(tier1row);
    tier1row.setNpcType("Administrator"); // tier 3: admin edit AFTER the stamp, seed says "Owner"

    // Samuel Winters's seed row (npcs.json): long referee description, type "Referee", expansion
    // "CUSTOM", awareness 85. The stamp carries the PREVIOUS release's awareness (50) while the
    // row shows the admin's edit (10) and the incoming seed ships 85 → genuine tier-2 collision
    // (stored 50 ≠ current 10, stored 50 ≠ new 85). All other fields align with the seed so the
    // fail-safe reconstruction reads them NO_OP.
    Npc tier2row = new Npc();
    tier2row.setName("Samuel Winters");
    tier2row.setDescription(SEED_SAMUEL_DESCRIPTION);
    tier2row.setNpcType("Referee");
    tier2row.setExpansionCode("CUSTOM");
    tier2row.getAttributes().put(NpcService.ATTRIBUTE_AWARENESS, 50);
    tier2row.setSeedFingerprint(npcFingerprint(tier2row));
    tier2row.getAttributes().put(NpcService.ATTRIBUTE_AWARENESS, 10); // tier 2: admin edit

    when(npcService.count()).thenReturn(2L);
    when(npcService.findAllUnfiltered()).thenReturn(List.of(tier1row, tier2row));

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              parkedCoordinates.add("#" + inv.getArgument(2));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    NpcSync detectionSync =
        new NpcSync("true", npcService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the seed value.
    assertEquals(
        "The mysterious owner of ATW.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged npcType edit preserved.
    assertEquals(
        "Administrator",
        tier1row.getNpcType(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited awareness kept its value; the row parked the awareness collision.
    assertEquals(
        10,
        tier2row.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS),
        "tier 2: admin-edited field must keep its value — only a PENDING decision is created");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("npc"),
            eq("npc:samuel-winters"),
            eq("Samuel Winters"),
            eq("awareness"),
            eq("10"),
            eq("85"),
            anyString());
    assertEquals(
        Set.of("npc:samuel-winters|awareness", "#Samuel Winters"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void versionChange_silentUpdatesCoverGuardedArms() {
    // Tier-1 write arms the three-tier pin never reached: the null-guarded expansionCode and
    // awareness arms plus the stamp-only save. Rezan the Resolved seeds at expansion "CUSTOM"
    // and awareness 100 (npcs.json); the row is stamped clean at expansion "OLD"/awareness 90,
    // so the seed's values classify SILENT_UPDATE and write through the guards. MVP is the
    // stamp-only row: clean, seed-unchanged, re-stamped from a manifest hash with nothing to
    // apply and nothing to park.
    Npc rezan = new Npc();
    rezan.setName("Rezan the Resolved");
    rezan.setDescription(
        "An ageless Matchkeeper from the Chrono-Monastery of the Unseen Year. Pale-skinned and"
            + " blindfolded with gold-plated runes, Rezan is an eternal arbiter summoned to"
            + " stabilize high-stakes cross-era contests. He is unwavering, impossible to"
            + " intimidate, and unaffected by distractions or chaos. When a match exceeds 12"
            + " turns, he utters 'It is decided,' and the next pin counts regardless of kickout"
            + " rolls.");
    rezan.setNpcType("Referee");
    rezan.setExpansionCode("OLD");
    rezan.getAttributes().put(NpcService.ATTRIBUTE_AWARENESS, 90);
    npcCanonicalHashed(rezan);

    Npc stampOnly = new Npc();
    stampOnly.setName("MVP");
    stampOnly.setDescription("");
    stampOnly.setNpcType("Manager");
    stampOnly.setExpansionCode("HURT_BUSINESS");
    npcCanonicalHashed(stampOnly);

    when(npcService.count()).thenReturn(2L);
    when(npcService.findAllUnfiltered()).thenReturn(List.of(rezan, stampOnly));

    SeedProvenanceManifest manifest =
        SeedProvenanceManifest.of(Map.of("npc", Map.of("MVP", "manifest-hash-mvp")));
    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              parkedCoordinates.add("#" + inv.getArgument(2));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    NpcSync detectionSync = new NpcSync("true", npcService, new ObjectMapper(), parking, manifest);
    detectionSync.detectVersionChangeCollisions();

    assertEquals(
        "CUSTOM", rezan.getExpansionCode(), "guarded expansionCode arm wrote the seed value");
    Mockito.verify(npcService).setAwareness(rezan, 100);
    Mockito.verify(npcService).saveAll(Mockito.argThat(list -> list.contains(rezan)));
    assertEquals(
        "manifest-hash-mvp",
        stampOnly.getSeedFingerprint(),
        "a clean unchanged row must adopt the manifest hash (stamp-only save)");
    Mockito.verify(npcService).saveAll(Mockito.argThat(list -> list.contains(stampOnly)));
    assertEquals(Set.of(), parkedCoordinates, "tier-1 and stamp-only rows never park a decision");
  }

  @Test
  void versionChangeDetection_toleratesBrokenSeedResource() {
    // Inner IOException catch: a seed file that fails to deserialize is logged and skipped.
    Npc existing = new Npc();
    existing.setName("Charles");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));
    ObjectMapper brokenMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(InputStream src, TypeReference<T> typeRef) throws IOException {
            throw new IOException("boom");
          }
        };
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    NpcSync detectionSync = new NpcSync("true", npcService, brokenMapper, parking, EMPTY_MANIFEST);
    Assertions.assertDoesNotThrow(detectionSync::detectVersionChangeCollisions);
    Mockito.verify(npcService, never()).saveAll(any());
    Mockito.verifyNoInteractions(parking);
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the pre-existing verify behavior still
    // holds with the new constructor dependencies.
    Npc existing = new Npc();
    existing.setName("Charles");
    existing.setDescription("stale description");
    when(npcService.count()).thenReturn(1L);
    Mockito.lenient().when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    NpcSync fullModeSync =
        new NpcSync("false", npcService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    fullModeSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(parking);
    assertEquals(
        "stale description",
        existing.getDescription(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }

  // ── Gender seed support (beads ATW-jbyo) ────────────────────────────────────

  @Test
  void sync_seededFemaleRefereesCarryTheirGender() {
    // The three female referees in npcs.json must land as FEMALE, not the entity's MALE default;
    // nothing consumes Npc.gender yet — this pins the data before a consumer exists.
    when(npcService.findAllUnfiltered()).thenReturn(List.of());

    sync.sync();

    ArgumentCaptor<List<Npc>> captor = ArgumentCaptor.forClass(List.class);
    verify(npcService, atLeastOnce()).saveAll(captor.capture());
    List<Npc> saved = captor.getAllValues().stream().flatMap(List::stream).toList();
    for (String female : List.of("Beatrix “Trixie” Halvorsen", "Dr. Yumi Sato", "Amara Diallo")) {
      Npc npc =
          saved.stream()
              .filter(n -> female.equals(n.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError(female + " must be seeded"));
      Assertions.assertEquals(Gender.FEMALE, npc.getGender(), female + " seeds as FEMALE");
    }
    // Male referees keep the default.
    Npc samuel =
        saved.stream().filter(n -> "Samuel Winters".equals(n.getName())).findFirst().orElseThrow();
    Assertions.assertEquals(Gender.MALE, samuel.getGender());
  }

  @Test
  void sync_fullModeUpdatesGenderOnExistingRow() {
    // A pre-existing row seeded before the gender field existed (stored as MALE) must adopt
    // the seed's gender on the next FULL pass.
    Npc trixie = new Npc();
    trixie.setName("Beatrix “Trixie” Halvorsen");
    trixie.setGender(Gender.MALE); // the pre-fix stored state
    when(npcService.findAllUnfiltered()).thenReturn(List.of(trixie));

    sync.sync();

    Assertions.assertEquals(
        Gender.FEMALE, trixie.getGender(), "FULL mode must adopt the seed's gender");
  }

  @Test
  void sync_genderChangeRestampsFingerprint() {
    // The stamp covers seed-owned fields; a gender correction on an existing row must refresh it.
    Npc yumi = new Npc();
    yumi.setName("Dr. Yumi Sato");
    yumi.setGender(Gender.MALE);
    yumi.setSeedFingerprint("stale-stamp-from-before-gender");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(yumi));

    sync.sync();

    Assertions.assertNotEquals("stale-stamp-from-before-gender", yumi.getSeedFingerprint());
    Assertions.assertNotNull(yumi.getSeedFingerprint(), "row re-stamps after the gender update");
  }

  @Test
  void sync_nullGenderRowDetectionToleratesUnsetGender() {
    // A pre-seed row with no gender at all must flow through fingerprinting, field
    // observations, and the canonical fingerprint order without NPEs — every gender slot
    // nulls out and the seed default (MALE) drives the "new value".
    Npc ghost = new Npc();
    ghost.setName("Ghost Announcer");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(ghost));

    sync.sync();

    verify(npcService, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_silentGenderUpdateWritesThroughSingleSave() {
    // A row whose ONLY divergence is gender hits the silent-update path: the seed's gender
    // wins (entity default MALE was stored), and the field writes through applySilentSeedFields.
    Npc mustafa = new Npc();
    mustafa.setName("Colonel Mustafa");
    mustafa.setGender(Gender.MALE);
    mustafa.setSeedFingerprint("stale-pre-gender-stamp");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(mustafa));

    sync.sync();

    verify(npcService, atLeastOnce()).saveAll(any());
  }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the relationships.json seed sync, including the version-change pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelationshipSyncTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerRelationshipRepository relationshipRepository;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private RelationshipSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(relationshipService.count()).thenReturn(0L);
  }

  private RelationshipSync sync(String mode) {
    return new RelationshipSync(
        mode,
        wrestlerRepository,
        relationshipRepository,
        relationshipService,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static Wrestler wrestler(String name) {
    return Wrestler.builder().name(name).build();
  }

  private static WrestlerRelationship relationship(
      Wrestler w1, Wrestler w2, RelationshipType type, int level, boolean storyline, String notes) {
    WrestlerRelationship r =
        WrestlerRelationship.builder()
            .wrestler1(w1)
            .wrestler2(w2)
            .type(type)
            .level(level)
            .isStoryline(storyline)
            .build();
    r.setNotes(notes);
    return r;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // TIER-2 + tier-3 pin, end-to-end through the wired pipeline, against the real seed file
    // (the detection pass reads relationships.json). The seeded row is the tier-2 leg: an
    // ADMIN-EDITED notes whose seed value ALSO changed since the stamp (previous release
    // shipped "previous seed note", the admin swapped to "admin's note", the incoming release
    // ships "Real life husband and wife"). Level and isStoryline equal the incoming seed on all
    // three vectors → the tier-3 legs: exactly the notes collision parks.
    Wrestler johnny = wrestler("Johnny All Time");
    Wrestler taya = wrestler("Taya Valkyrie");
    // current level 100 == the seed's level; notes carry the admin's edit.
    WrestlerRelationship row =
        relationship(johnny, taya, RelationshipType.SPOUSE, 100, false, "admin's note");
    // The stamp carries the PREVIOUS release's seed notes while the row shows the admin's edit
    // and the incoming seed ships a third value → genuine tier-2 collision.
    row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("100", "false", "previous seed note"));

    when(relationshipService.count()).thenReturn(1L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(johnny, taya));
    when(relationshipRepository.findAllWithWrestlers()).thenReturn(List.of(row));

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

    RelationshipSync detectionSync =
        new RelationshipSync(
            "true",
            wrestlerRepository,
            relationshipRepository,
            relationshipService,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 2: the admin's note kept its value; the row parked the collision.
    assertEquals("admin's note", row.getNotes(), "tier 2: admin edit must keep its value");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            eq("wrestler_relationship"),
            eq("relationship:johnny-all-time<SPOUSE>taya-valkyrie"),
            eq("Johnny All Time <SPOUSE> Taya Valkyrie"),
            eq("notes"),
            eq("admin's note"),
            eq("Real life husband and wife"),
            anyString());
    assertEquals(
        java.util.Set.of("relationship:johnny-all-time<SPOUSE>taya-valkyrie|notes"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
    // The parked row is never written.
    verify(relationshipRepository, never()).saveAll(any());
  }

  @Test
  void versionChangeAppliesTier1Silently() {
    // TIER-1 pin on the same seeded row: a CLEAN row whose stamp hashes its current
    // (previous-seed) vector, with the incoming seed shipping a changed level and storyline →
    // both update silently and the row re-stamps to the incoming fingerprint.
    Wrestler johnny = wrestler("Johnny All Time");
    Wrestler taya = wrestler("Taya Valkyrie");
    // Previous seed shipped level 60, storyline true, notes as today; the incoming seed
    // rebalances to 100/false.
    WrestlerRelationship row =
        relationship(johnny, taya, RelationshipType.SPOUSE, 60, true, "Real life husband and wife");
    row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("60", "true", "Real life husband and wife"));

    when(relationshipService.count()).thenReturn(1L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(johnny, taya));
    when(relationshipRepository.findAllWithWrestlers()).thenReturn(List.of(row));

    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    RelationshipSync detectionSync =
        new RelationshipSync(
            "true",
            wrestlerRepository,
            relationshipRepository,
            relationshipService,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched level and storyline updated silently to the NEW seed values.
    assertEquals(100, row.getLevel(), "tier 1: seed's new level must apply silently");
    assertEquals(false, row.getIsStoryline(), "tier 1: seed's new storyline must apply silently");
    verify(parking, never())
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    RelationshipSync fullSync = sync("false");
    when(relationshipService.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(relationshipRepository, never()).findAllWithWrestlers();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row: the service's createOrUpdate leaves the
    // stamp unset, and the sync stamps it through the repository.
    Wrestler johnny = wrestler("Johnny All Time");
    Wrestler taya = wrestler("Taya Valkyrie");
    WrestlerRelationship seeded =
        relationship(
            johnny, taya, RelationshipType.SPOUSE, 100, false, "Real life husband and wife");
    when(relationshipService.count()).thenReturn(0L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(johnny, taya));
    when(relationshipService.createOrUpdateRelationship(
            any(), any(), any(), anyInt(), anyBoolean(), any()))
        .thenReturn(seeded);
    when(relationshipRepository.findBetweenWrestlers(johnny, taya)).thenReturn(List.of(seeded));

    sync.sync();

    verify(relationshipRepository, org.mockito.Mockito.atLeastOnce())
        .save(org.mockito.ArgumentMatchers.argThat(r -> r.getSeedFingerprint() != null));
  }
}

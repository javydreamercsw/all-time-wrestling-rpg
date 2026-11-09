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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the commentators.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentatorSyncTest {

  @Mock private CommentaryService commentaryService;
  @Mock private CommentatorRepository commentatorRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private CommentatorSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(commentaryService.countCommentators()).thenReturn(0L);
  }

  private CommentatorSync sync(String mode) {
    return new CommentatorSync(
        mode,
        commentaryService,
        commentatorRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static Commentator commentator(String npcName, String style, String catchphrase) {
    Npc npc = new Npc();
    npc.setName(npcName);
    npc.setDescription("description");
    npc.setNpcType("Commentator");
    Commentator c = new Commentator();
    c.setNpc(npc);
    c.setStyle(style);
    c.setCatchphrase(catchphrase);
    return c;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. "Dara Hoshiko" is the tier-1 row
    // (style stale vs the stamp, seed current); "Lord Bastian Von Crowe" the tier-2 row
    // (admin-tuned catchphrase while the seed also changed it). Every other field of both rows
    // matches the incoming seed exactly — a row-level stamp reconstructs per-field provenance only
    // when the dirty row is a single edit away from the seed vector.
    String daraCatchphrase = "Victory is timeless — and tonight, it belongs to the brave!";
    String crowePersona =
        "Style: 'Oh please, that suplex was as clumsy as a Viking in ballet slippers. I’ve seen"
            + " more grace at a medieval bloodletting!'";
    Commentator tier1row = commentator("Dara Hoshiko", "stale style", daraCatchphrase);
    tier1row.setPersonaDescription(
        "Signature style: 'This isn't just a match, this is a moment written across the centuries"
            + " — and folks, I think we're witnessing history!'");
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "stale style", daraCatchphrase, tier1row.getPersonaDescription()));

    Commentator tier2row =
        commentator("Lord Bastian Von Crowe", "Color / Theatrical", "admin's catchphrase");
    tier2row.setPersonaDescription(crowePersona);
    // The stamp carries the PREVIOUS release's seed catchphrase while the row shows the admin's
    // edit and the incoming seed ships a third value → genuine tier-2 collision.
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "Color / Theatrical", "previous seed catchphrase", crowePersona));

    when(commentaryService.countCommentators()).thenReturn(2L);
    when(commentatorRepository.findAll()).thenReturn(List.of(tier1row, tier2row));

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

    CommentatorSync detectionSync =
        new CommentatorSync(
            "true",
            commentaryService,
            commentatorRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched style updated silently to the NEW seed value.
    assertEquals(
        "Play-by-play / Analytical",
        tier1row.getStyle(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's catchphrase kept its value; the row parked the collision.
    assertEquals(
        "admin's catchphrase", tier2row.getCatchphrase(), "tier 2: admin edit must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("commentator"),
            eq("commentator:lord-bastian-von-crowe"),
            eq("Lord Bastian Von Crowe"),
            eq("catchphrase"),
            eq("admin's catchphrase"),
            eq("Champions rise… but villains endure."),
            anyString());
    assertEquals(
        Set.of("commentator:lord-bastian-von-crowe|catchphrase", "#Lord Bastian Von Crowe"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    CommentatorSync fullSync = sync("false");
    when(commentaryService.countCommentators()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(commentatorRepository, never()).findAll();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row: the service's createOrUpdate leaves the
    // stamp unset, and the sync stamps it through the repository.
    when(commentaryService.countCommentators()).thenReturn(0L);
    Commentator seeded = commentator("Dara Hoshiko", "Play-by-play / Analytical", "catchphrase");
    when(commentaryService.createOrUpdateCommentator(
            anyString(), any(), any(), any(), any(), any(), any(), anyString()))
        .thenReturn(seeded);
    when(commentaryService.findCommentatorByNpcName(anyString())).thenReturn(Optional.of(seeded));

    sync.sync();

    verify(commentatorRepository, Mockito.atLeastOnce())
        .save(ArgumentMatchers.argThat(c -> c.getSeedFingerprint() != null));
  }
}

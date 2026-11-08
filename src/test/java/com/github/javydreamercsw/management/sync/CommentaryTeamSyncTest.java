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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
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

/** Unit tests for the commentary_teams.json seed sync, including the version-change pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentaryTeamSyncTest {

  @Mock private CommentaryService commentaryService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private CommentaryTeamSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(commentaryService.countTeams()).thenReturn(0L);
  }

  private CommentaryTeamSync sync(String mode) {
    return new CommentaryTeamSync(
        mode,
        commentaryService,
        commentaryTeamRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static CommentaryTeam team(String name, List<String> memberNames) {
    CommentaryTeam t = new CommentaryTeam();
    t.setName(name);
    memberNames.forEach(
        n -> {
          Npc npc = new Npc();
          npc.setName(n);
          Commentator c = new Commentator();
          c.setNpc(npc);
          t.getCommentators().add(c);
        });
    return t;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // TIER-2 pin, end-to-end through the wired pipeline, against the real seed file (the
    // detection pass reads commentary_teams.json). The domain's single seed-owned field is the
    // membership (canonicalized sorted), so the tiers live across TESTS on the one seeded row:
    // this test pins the tier-2 leg — an ADMIN-RESTAFFED booth whose seed membership ALSO
    // changed since the stamp. The clean and single-edit reconstructions both fail → fail-safe,
    // the members collision parks with the exact (current, new) pair, the row's value and stamp
    // stay untouched (tier-3 leg: nothing else on the row is touched either).
    CommentaryTeam tier2row =
        team("All-Time Broadcast Team", List.of("Admin Pick One", "Admin Pick Two"));
    // The stamp carries the PREVIOUS release's seed membership while the row shows the admin's
    // picks and the incoming seed ships the catalog vector → genuine tier-2 collision.
    tier2row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("Old Seed Member"));

    when(commentaryService.countTeams()).thenReturn(1L);
    when(commentaryTeamRepository.findAll()).thenReturn(List.of(tier2row));

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

    CommentaryTeamSync detectionSync =
        new CommentaryTeamSync(
            "true",
            commentaryService,
            commentaryTeamRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 2: the admin's membership kept its value; the row parked the collision with the exact
    // (current, new) pair — the row-level fail-safe parks the whole members field.
    assertEquals(
        List.of("Admin Pick One", "Admin Pick Two"),
        tier2row.getCommentators().stream().map(c -> c.getNpc().getName()).toList(),
        "tier 2: admin membership must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("commentary_team"),
            eq("commentary_team:all-time-broadcast-team"),
            eq("All-Time Broadcast Team"),
            eq("members"),
            eq("Admin Pick One\nAdmin Pick Two"),
            eq("Dara Hoshiko\nLord Bastian Von Crowe"),
            anyString());
    assertEquals(
        Set.of("commentary_team:all-time-broadcast-team|members"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
    // The parked row is never written (the decision owns any later value change).
    verify(commentaryService, never()).createOrUpdateTeam(anyString(), anyList());
    verify(commentaryTeamRepository, never()).saveAll(any());
  }

  @Test
  void versionChangeAppliesTier1Silently() {
    // TIER-1 pin on the same seeded row: a CLEAN row whose stamp hashes its current (previous
    // seed) membership, with the incoming seed shipping a renamed membership → the update
    // applies silently through the service and the row re-stamps to the incoming fingerprint.
    CommentaryTeam tier1row =
        team("All-Time Broadcast Team", List.of("Dara Hoshiko", "Lord Crowe"));
    tier1row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("Dara Hoshiko\nLord Crowe"));

    when(commentaryService.countTeams()).thenReturn(1L);
    when(commentaryTeamRepository.findAll()).thenReturn(List.of(tier1row));

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    CommentaryTeamSync detectionSync =
        new CommentaryTeamSync(
            "true",
            commentaryService,
            commentaryTeamRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the stale membership updated silently to the NEW seed vector (the service write
    // carries the seed's member list).
    verify(commentaryService)
        .createOrUpdateTeam(
            eq("All-Time Broadcast Team"), eq(List.of("Dara Hoshiko", "Lord Bastian Von Crowe")));
    verify(parking, never())
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    CommentaryTeamSync fullSync = sync("false");
    when(commentaryService.countTeams()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(commentaryTeamRepository, never()).findAll();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row: the service's createOrUpdateTeam leaves the
    // stamp unset, and the sync stamps it through the repository.
    when(commentaryService.countTeams()).thenReturn(0L);
    CommentaryTeam seeded = team("All-Time Broadcast Team", List.of("A", "B"));
    when(commentaryService.findTeamByName(anyString())).thenReturn(Optional.of(seeded));

    sync.sync();

    verify(commentaryTeamRepository, Mockito.atLeastOnce())
        .save(ArgumentMatchers.argThat(t -> t.getSeedFingerprint() != null));
  }
}

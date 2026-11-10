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
package com.github.javydreamercsw.management.sync.review;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.sync.AchievementSync;
import com.github.javydreamercsw.management.sync.ArenaSync;
import com.github.javydreamercsw.management.sync.CampaignAbilityCardSync;
import com.github.javydreamercsw.management.sync.CampaignUpgradeSync;
import com.github.javydreamercsw.management.sync.CardSetSync;
import com.github.javydreamercsw.management.sync.CardSync;
import com.github.javydreamercsw.management.sync.ChampionshipSync;
import com.github.javydreamercsw.management.sync.CommentaryTeamSync;
import com.github.javydreamercsw.management.sync.CommentatorSync;
import com.github.javydreamercsw.management.sync.DeckSync;
import com.github.javydreamercsw.management.sync.FactionSync;
import com.github.javydreamercsw.management.sync.LocationSync;
import com.github.javydreamercsw.management.sync.NpcSync;
import com.github.javydreamercsw.management.sync.OutcomeMatrixSync;
import com.github.javydreamercsw.management.sync.RelationshipSync;
import com.github.javydreamercsw.management.sync.RingsideActionSync;
import com.github.javydreamercsw.management.sync.SegmentRuleSync;
import com.github.javydreamercsw.management.sync.SegmentTypeSync;
import com.github.javydreamercsw.management.sync.ShowTemplateSync;
import com.github.javydreamercsw.management.sync.ShowTypeSync;
import com.github.javydreamercsw.management.sync.StatusCardSync;
import com.github.javydreamercsw.management.sync.TeamSync;
import com.github.javydreamercsw.management.sync.TournamentSync;
import com.github.javydreamercsw.management.sync.WrestlerSync;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit pins for the boot-time seed-review detection orchestrator (PR 4 final wiring).
 *
 * <p>Real Mockito mocks for the {@link SeedUpdateReviewService} and all 24 content syncs. The pass
 * mechanics pinned here (gate checked once, per-domain failure isolation with stamp withheld, stamp
 * after a clean pass, stamp on a skipped pass, notification close after the pass) are list-position
 * independent; each sync's own three-tier detection contract is pinned in its {@code *SyncTest}.
 */
@ExtendWith(MockitoExtension.class)
class SeedReviewDetectionOrchestratorTest {

  @Mock private SeedUpdateReviewService reviewService;
  @Mock private SegmentRuleSync segmentRuleSync;
  @Mock private ShowTypeSync showTypeSync;
  @Mock private SegmentTypeSync segmentTypeSync;
  @Mock private TournamentSync tournamentSync;
  @Mock private ShowTemplateSync showTemplateSync;
  @Mock private CardSetSync cardSetSync;
  @Mock private CardSync cardSync;
  @Mock private NpcSync npcSync;
  @Mock private LocationSync locationSync;
  @Mock private ArenaSync arenaSync;
  @Mock private WrestlerSync wrestlerSync;
  @Mock private RelationshipSync relationshipSync;
  @Mock private ChampionshipSync championshipSync;
  @Mock private DeckSync deckSync;
  @Mock private FactionSync factionSync;
  @Mock private TeamSync teamSync;
  @Mock private CampaignAbilityCardSync campaignAbilityCardSync;
  @Mock private StatusCardSync statusCardSync;
  @Mock private CampaignUpgradeSync campaignUpgradeSync;
  @Mock private CommentatorSync commentatorSync;
  @Mock private CommentaryTeamSync commentaryTeamSync;
  @Mock private AchievementSync achievementSync;
  @Mock private RingsideActionSync ringsideActionSync;
  @Mock private OutcomeMatrixSync outcomeMatrixSync;

  private SeedReviewDetectionOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    orchestrator = new OrchestratorFixture().build();
  }

  /** Wires the 24 mocked syncs into the orchestrator in the production order. */
  private class OrchestratorFixture {
    SeedReviewDetectionOrchestrator build() {
      return new SeedReviewDetectionOrchestrator(
          reviewService,
          segmentRuleSync,
          showTypeSync,
          segmentTypeSync,
          tournamentSync,
          showTemplateSync,
          cardSetSync,
          cardSync,
          npcSync,
          locationSync,
          arenaSync,
          wrestlerSync,
          relationshipSync,
          championshipSync,
          deckSync,
          factionSync,
          teamSync,
          campaignAbilityCardSync,
          statusCardSync,
          campaignUpgradeSync,
          commentatorSync,
          commentaryTeamSync,
          achievementSync,
          ringsideActionSync,
          outcomeMatrixSync);
    }
  }

  @Test
  void versionChangeRunsEveryDomainThenStampsTheVersion() {
    when(reviewService.shouldRunDetection()).thenReturn(true);

    orchestrator.detectIfVersionChanged();

    verify(segmentRuleSync).detectVersionChangeCollisions();
    verify(showTypeSync).detectVersionChangeCollisions();
    verify(segmentTypeSync).detectVersionChangeCollisions();
    verify(tournamentSync).detectVersionChangeCollisions();
    verify(showTemplateSync).detectVersionChangeCollisions();
    verify(cardSetSync).detectVersionChangeCollisions();
    verify(cardSync).detectVersionChangeCollisions();
    verify(npcSync).detectVersionChangeCollisions();
    verify(locationSync).detectVersionChangeCollisions();
    verify(arenaSync).detectVersionChangeCollisions();
    verify(wrestlerSync).detectVersionChangeCollisions();
    verify(relationshipSync).detectVersionChangeCollisions();
    verify(championshipSync).detectVersionChangeCollisions();
    verify(deckSync).detectVersionChangeCollisions();
    verify(factionSync).detectVersionChangeCollisions();
    verify(teamSync).detectVersionChangeCollisions();
    verify(campaignAbilityCardSync).detectVersionChangeCollisions();
    verify(statusCardSync).detectVersionChangeCollisions();
    verify(campaignUpgradeSync).detectVersionChangeCollisions();
    verify(commentatorSync).detectVersionChangeCollisions();
    verify(commentaryTeamSync).detectVersionChangeCollisions();
    verify(achievementSync).detectVersionChangeCollisions();
    verify(ringsideActionSync).detectVersionChangeCollisions();
    verify(outcomeMatrixSync).detectVersionChangeCollisions();
    verify(reviewService).markDetected();
  }

  @Test
  void gateCheckedOnceBeforeThePass() {
    when(reviewService.shouldRunDetection()).thenReturn(true);

    orchestrator.detectIfVersionChanged();

    // Exactly one gate consult for the whole pass — not one per domain.
    verify(reviewService).shouldRunDetection();
  }

  @Test
  void skippedPassRunsNoDomainAndStillStampsTheVersion() {
    when(reviewService.shouldRunDetection()).thenReturn(false);

    orchestrator.detectIfVersionChanged();

    verifyNoInteractions(
        segmentRuleSync,
        showTypeSync,
        segmentTypeSync,
        tournamentSync,
        showTemplateSync,
        cardSetSync,
        cardSync,
        npcSync,
        locationSync,
        arenaSync,
        wrestlerSync,
        relationshipSync,
        championshipSync,
        deckSync,
        factionSync,
        teamSync,
        campaignAbilityCardSync,
        statusCardSync,
        campaignUpgradeSync,
        commentatorSync,
        commentaryTeamSync,
        achievementSync,
        ringsideActionSync,
        outcomeMatrixSync);
    verify(reviewService, never()).notifyPending();
    // Stamp liveness: markDetected() is the only writer of the stamp shouldRunDetection()
    // compares against, and its absence branch treats "no stamp" as NOT a version change —
    // writing it only inside the gated pass would leave the first stamp unwritable forever.
    verify(reviewService).markDetected();
  }

  @Test
  void oneDomainFailingDoesNotPreventTheOtherDomainsFromRunning() {
    when(reviewService.shouldRunDetection()).thenReturn(true);
    doThrow(new IllegalStateException("wrestler detection exploded"))
        .when(wrestlerSync)
        .detectVersionChangeCollisions();

    orchestrator.detectIfVersionChanged();

    // Every domain ran — the failure was caught, not propagated.
    verify(wrestlerSync).detectVersionChangeCollisions();
    verify(factionSync).detectVersionChangeCollisions();
    verify(outcomeMatrixSync).detectVersionChangeCollisions();
    verify(commentatorSync).detectVersionChangeCollisions();
    // Pinned ruling: with any domain failed the stamp is WITHHELD — the next boot re-runs the
    // full pass (safe: tier-1 writes converge, parks dedupe idempotently) rather than losing
    // that detection window until the next version bump.
    verify(reviewService, never()).markDetected();
    // The notification close still fires so parks completed before the failure deliver.
    verify(reviewService).notifyPending();
  }

  @Test
  void markDetectedStampsOnlyAfterEveryDomainRan() {
    when(reviewService.shouldRunDetection()).thenReturn(true);

    orchestrator.detectIfVersionChanged();

    InOrder inOrder = inOrder(reviewService, factionSync, outcomeMatrixSync);
    inOrder.verify(factionSync).detectVersionChangeCollisions();
    inOrder.verify(outcomeMatrixSync).detectVersionChangeCollisions();
    inOrder.verify(reviewService).notifyPending();
    inOrder.verify(reviewService).markDetected();
  }
}

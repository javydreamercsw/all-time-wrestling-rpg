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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs the wired content domains' version-change detection pass at boot (seed-update review
 * feature) and owns the phase bookkeeping around it: the once-per-phase {@link
 * SeedUpdateReviewService#notifyPending()} close and the detected-version stamp. Called from {@code
 * DataInitializer.performInit()} between {@code runMigrations()} and {@code
 * forceResolveIfConfigured()} — after the sync phase (all domain passes complete, stamp top-ups
 * committed, so detection classifies rows that already carry this boot's stamps) and before
 * force-resolve (a keep-all escape decides only after this boot's new collisions are parked).
 *
 * <h2>Gate</h2>
 *
 * {@link SeedUpdateReviewService#shouldRunDetection()} is checked exactly ONCE, up front: within a
 * single boot the app version is fixed, and re-consulting the PENDING-count leg per domain would
 * let parks made by earlier domains of the same pass widen the pass mid-flight. When it is false,
 * no domain runs at all. The orchestrator never touches {@link SeedReviewGate} — parking raises the
 * gate from inside {@code park*}; applying decisions lowers it from the review UI.
 *
 * <h2>The 24 domains</h2>
 *
 * Each wired content sync exposes {@code detectVersionChangeCollisions()} as its version-changed
 * branch (three-tier classification: tier-1 silent update + re-stamp, tier-2 park, tier-3
 * preserve). The syncs are injected individually — not swept from the {@code DataSyncContributor}
 * list — so the pass runs exactly the reviewed content domains in a fixed order (here: each sync's
 * {@code @Order} value) regardless of what else registers as a contributor. The methods are {@code
 * public} solely as orchestrator entry points; no other production code calls them.
 *
 * <h2>Failure handling (pinned ruling)</h2>
 *
 * A single domain's detection failure is caught and logged with its domain name; the loop
 * CONTINUES, so the other 23 domains still run their pass. If at least one domain failed, the
 * detected-version stamp is WITHHELD for this boot: {@code markDetected()} is not called, so the
 * next boot sees the previous stamp and re-runs the full pass — deliberate, because a detection
 * window lost to stamping-after-failure never comes back until the next version bump, whereas
 * re-running is safe (tier-1 writes converge; parks dedupe idempotently) and merely noisy until the
 * failure clears. A boot whose pass completes cleanly stamps the version exactly once, after all
 * domains ran.
 *
 * <h2>Stamp liveness (markDetected once per boot)</h2>
 *
 * On a boot where {@code shouldRunDetection()} is false the pass is skipped entirely, yet {@code
 * markDetected()} is still called once at the end. That is deliberate: the stamp is the ONLY
 * writer's output {@code shouldRunDetection()} compares against, and its "no stamp yet" branch
 * treats a missing stamp as NOT a version change — if the stamp were written only inside the gated
 * pass, no boot could ever write the first stamp (fresh install: nothing pending, no stamp → skip →
 * still no stamp), the version-change trigger would stay dead for the install's whole lifetime, and
 * the sandbox upgrade verification could never pass. The call is an idempotent upsert: on a
 * same-version boot it writes nothing; on a fresh install it lays the bootstrap stamp this
 * feature's own javadoc contract requires ("called exactly once per boot, after the version-change
 * detection pass has finished").
 */
@Slf4j
@Component
public class SeedReviewDetectionOrchestrator {

  /** The wired content domains in fixed pass order (mirrors each sync's {@code @Order}). */
  private final List<ContentDomain> contentDomains;

  private final SeedUpdateReviewService reviewService;

  /** One wired content domain: its sync bean and the name used in pass logging. */
  record ContentDomain(String domainName, Runnable detection) {}

  @SuppressWarnings("java:S107") // 24 explicit sync beans — the pass runs exactly these, in order
  public SeedReviewDetectionOrchestrator(
      final SeedUpdateReviewService reviewService,
      final SegmentRuleSync segmentRuleSync,
      final ShowTypeSync showTypeSync,
      final SegmentTypeSync segmentTypeSync,
      final TournamentSync tournamentSync,
      final ShowTemplateSync showTemplateSync,
      final CardSetSync cardSetSync,
      final CardSync cardSync,
      final NpcSync npcSync,
      final LocationSync locationSync,
      final ArenaSync arenaSync,
      final WrestlerSync wrestlerSync,
      final RelationshipSync relationshipSync,
      final ChampionshipSync championshipSync,
      final DeckSync deckSync,
      final FactionSync factionSync,
      final TeamSync teamSync,
      final CampaignAbilityCardSync campaignAbilityCardSync,
      final StatusCardSync statusCardSync,
      final CampaignUpgradeSync campaignUpgradeSync,
      final CommentatorSync commentatorSync,
      final CommentaryTeamSync commentaryTeamSync,
      final AchievementSync achievementSync,
      final RingsideActionSync ringsideActionSync,
      final OutcomeMatrixSync outcomeMatrixSync) {
    this.reviewService = reviewService;
    this.contentDomains =
        List.of(
            new ContentDomain(
                SegmentRuleSync.DOMAIN_KEY, segmentRuleSync::detectVersionChangeCollisions),
            new ContentDomain(ShowTypeSync.DOMAIN_KEY, showTypeSync::detectVersionChangeCollisions),
            new ContentDomain(
                SegmentTypeSync.DOMAIN_KEY, segmentTypeSync::detectVersionChangeCollisions),
            new ContentDomain(
                TournamentSync.DOMAIN_KEY, tournamentSync::detectVersionChangeCollisions),
            new ContentDomain(
                ShowTemplateSync.DOMAIN_KEY, showTemplateSync::detectVersionChangeCollisions),
            new ContentDomain(CardSetSync.DOMAIN_KEY, cardSetSync::detectVersionChangeCollisions),
            new ContentDomain(CardSync.DOMAIN_KEY, cardSync::detectVersionChangeCollisions),
            new ContentDomain(NpcSync.DOMAIN_KEY, npcSync::detectVersionChangeCollisions),
            new ContentDomain(LocationSync.DOMAIN_KEY, locationSync::detectVersionChangeCollisions),
            new ContentDomain(ArenaSync.DOMAIN_KEY, arenaSync::detectVersionChangeCollisions),
            new ContentDomain(WrestlerSync.DOMAIN_KEY, wrestlerSync::detectVersionChangeCollisions),
            new ContentDomain(
                RelationshipSync.DOMAIN_KEY, relationshipSync::detectVersionChangeCollisions),
            new ContentDomain(
                ChampionshipSync.DOMAIN_KEY, championshipSync::detectVersionChangeCollisions),
            new ContentDomain(DeckSync.DOMAIN_KEY, deckSync::detectVersionChangeCollisions),
            new ContentDomain(FactionSync.DOMAIN_KEY, factionSync::detectVersionChangeCollisions),
            new ContentDomain(TeamSync.DOMAIN_KEY, teamSync::detectVersionChangeCollisions),
            new ContentDomain(
                CampaignAbilityCardSync.DOMAIN_KEY,
                campaignAbilityCardSync::detectVersionChangeCollisions),
            new ContentDomain(
                StatusCardSync.DOMAIN_KEY, statusCardSync::detectVersionChangeCollisions),
            new ContentDomain(
                CampaignUpgradeSync.DOMAIN_KEY, campaignUpgradeSync::detectVersionChangeCollisions),
            new ContentDomain(
                CommentatorSync.DOMAIN_KEY, commentatorSync::detectVersionChangeCollisions),
            new ContentDomain(
                CommentaryTeamSync.DOMAIN_KEY, commentaryTeamSync::detectVersionChangeCollisions),
            new ContentDomain(
                AchievementSync.DOMAIN_KEY, achievementSync::detectVersionChangeCollisions),
            new ContentDomain(
                RingsideActionSync.DOMAIN_KEY, ringsideActionSync::detectVersionChangeCollisions),
            new ContentDomain(
                OutcomeMatrixSync.DOMAIN_KEY, outcomeMatrixSync::detectVersionChangeCollisions));
  }

  /**
   * The boot-time detection phase: gate once, run every wired domain's version-changed branch
   * (catch-and-log per domain), close the phase with the batched pending notification when the pass
   * ran, and stamp the detected version when the pass ran clean. See the class javadoc for the
   * failure-withhold and stamp-liveness rulings.
   */
  public void detectIfVersionChanged() {
    if (!reviewService.shouldRunDetection()) {
      log.debug("Seed review detection not needed this boot — skipping the domain pass.");
      stampDetectedVersion(0);
      return;
    }
    log.info(
        "Seed review detection pass starting across {} content domains.", contentDomains.size());
    int failures = 0;
    for (ContentDomain domain : contentDomains) {
      try {
        domain.detection().run();
      } catch (RuntimeException e) {
        failures++;
        log.error(
            "Seed review detection failed for domain '{}' — continuing with the remaining"
                + " domains; the version stamp is withheld this boot so the pass re-runs next"
                + " boot.",
            domain.domainName(),
            e);
      }
    }
    log.info(
        "Seed review detection pass finished: {} domain(s), {} failure(s).",
        contentDomains.size(),
        failures);
    // Close the parking phase even when some domains failed: parks those domains completed
    // before failing must still deliver the administrator notification. Inert when nothing
    // parked (the once-per-boot flag is unarmed).
    reviewService.notifyPending();
    stampDetectedVersion(failures);
  }

  /**
   * Stamps the detected version exactly when the boot's pass completed without failures (or no pass
   * was needed): the upsert is a no-op on same-version boots and the first-stamp bootstrap on fresh
   * installs. Withheld while any domain failed — see the class javadoc ruling.
   */
  private void stampDetectedVersion(int failures) {
    if (failures > 0) {
      log.warn(
          "Seed review detection had {} failed domain(s) — detected-version stamp withheld;"
              + " the full pass re-runs on the next boot.",
          failures);
      return;
    }
    reviewService.markDetected();
  }
}

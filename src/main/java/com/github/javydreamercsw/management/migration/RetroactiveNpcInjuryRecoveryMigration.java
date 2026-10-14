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
package com.github.javydreamercsw.management.migration;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameClock;
import com.github.javydreamercsw.utils.DiceBag;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time repair (ATW-3qug) for injuries that piled up while the post-show resting loop used the
 * non-rules bump-only {@code healChance} — NPCs never healed injuries at all, and production
 * accumulated 19 active injuries (the oldest 47+ game days old).
 *
 * <p>For each still-active injury this replays the missed recovery opportunities: every show dated
 * after the injury where the wrestler RESTED (no non-promo segment participation — promos never
 * count, matching {@code ShowService#adjudicateShow}'s participation rule) in a non-campaign
 * universe (campaign healing runs through BackstageActionService RECOVERY). Each missed show gets
 * one ATW recovery roll with the same semantics as the live loop ({@code
 * WrestlerService#performNpcRecovery}): one die per Resilience point, 4+ per die is a success, 2+
 * successes heal the injury (severity-independent). The heal is stamped on the show's game date.
 *
 * <p>Per the repair contract the replay for a given injury STOPS the moment that injury heals —
 * afterwards the wrestler no longer carries it, so later shows would not have offered that roll.
 *
 * <p>Bump removals are NOT replayed: bumps are a separate counter and production bumps were already
 * decaying under the old code; inventing bump history adds noise without fixing the reported
 * problem. Exactly-once via {@code data_migration_history}.
 */
@Component
@Order(30)
@RequiredArgsConstructor
@Slf4j
public class RetroactiveNpcInjuryRecoveryMigration implements DataMigration {

  private final InjuryRepository injuryRepository;
  private final ShowRepository showRepository;
  private final SegmentRepository segmentRepository;
  private final CampaignRepository campaignRepository;
  private final GameClock gameClock;

  @Override
  public String id() {
    return "retroactive-npc-injury-recovery";
  }

  @Override
  @Transactional
  public void migrate() {
    List<Injury> activeInjuries = injuryRepository.findHealableInjuries();
    if (activeInjuries.isEmpty()) {
      log.info("No active injuries found — retroactive recovery has nothing to do.");
      return;
    }

    List<Show> allShows = showRepository.findAll();
    log.info(
        "Replaying missed NPC recovery rolls for {} active injur(ies) against {} show(s)",
        activeInjuries.size(),
        allShows.size());

    int healed = 0;
    for (Injury injury : activeInjuries) {
      if (!injury.isCurrentlyActive()) {
        continue; // healed by an earlier replay sharing the same wrestler
      }
      Wrestler wrestler = injury.getWrestler();
      if (wrestler == null || injury.getUniverse() == null) {
        log.warn(
            "Injury {} has no wrestler or universe — retroactive recovery skipped", injury.getId());
        continue;
      }
      Long universeId = injury.getUniverse().getId();
      LocalDate injuryDate = injury.getInjuryDate().atZone(ZoneOffset.UTC).toLocalDate();

      // The shows where this wrestler actually worked (non-promo) since the injury — they were
      // NOT resting there, so no recovery roll was owed. Everything else in the window was.
      Set<Long> workedShowIds =
          segmentRepository.findNonPromoParticipationAfter(wrestler, injuryDate).stream()
              .map(Segment::getShow)
              .filter(show -> show.getShowDate() != null)
              .map(Show::getId)
              .collect(Collectors.toSet());

      List<Show> missedShows =
          allShows.stream()
              .filter(show -> show.getShowDate() != null)
              .filter(show -> !show.getShowDate().isBefore(injuryDate))
              .filter(show -> workedShowIds.stream().noneMatch(id -> id.equals(show.getId())))
              .filter(
                  show ->
                      show.getUniverse() == null
                          || !campaignRepository.existsByUniverse(show.getUniverse()))
              .toList();

      for (Show missed : missedShows) {
        int resilience = wrestler.getResilience() != null ? wrestler.getResilience() : 1;
        DiceBag diceBag = recoveryDiceBag(resilience);
        int successes = countSuccesses(diceBag);

        if (successes >= 2) {
          injury.setHealedDate(gameClock.gameNowStaggeredOn(missed.getShowDate()));
          injuryRepository.save(injury);
          healed++;
          log.info(
              "Retroactive recovery: injury '{}' (id {}) of {} healed on replayed show '{}' "
                  + "({} successes)",
              injury.getName(),
              injury.getId(),
              wrestler.getName(),
              missed.getName(),
              successes);
          break; // ATW-3qug: the injury no longer exists — stop replaying shows for it
        }
      }
    }
    log.info(
        "Retroactive NPC injury recovery complete: {} of {} active injur(ies) healed",
        healed,
        activeInjuries.size());
  }

  /** ATW recovery roll: each die showing 4+ counts as one success. */
  private int countSuccesses(DiceBag diceBag) {
    diceBag.roll();
    int[] rolls = diceBag.getLastRoll() != null ? diceBag.getLastRoll() : new int[0];
    int successes = 0;
    for (int roll : rolls) {
      if (roll >= 4) {
        successes++;
      }
    }
    return successes;
  }

  /**
   * One d6 per Resilience point (minimum one die). Package-private for tests to override with a
   * deterministic bag.
   */
  DiceBag recoveryDiceBag(int resilience) {
    int[] dice = new int[Math.max(1, resilience)];
    Arrays.fill(dice, 6);
    return new DiceBag(dice);
  }
}

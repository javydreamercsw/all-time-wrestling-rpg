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
package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchRewardServiceImpl implements MatchRewardService {

  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final LegacyService legacyService;
  private final Random random = new Random();

  @Override
  @Transactional
  public void processRewards(Segment segment, double difficultyMultiplier) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();

    if (!segment.getSegmentType().getName().equals("Promo")) {
      handleMatchRewards(segment, winners, losers, roll, difficultyMultiplier);
    } else {
      handlePromoRewards(segment, roll, difficultyMultiplier);
    }
  }

  private void handleMatchRewards(
      Segment segment,
      List<Wrestler> winners,
      List<Wrestler> losers,
      int roll,
      double difficultyMultiplier) {
    int matchQualityBonus = calculateMatchQualityBonus(segment, roll);

    // Deduct fan fees for challengers in title segments
    if (segment.getIsTitleSegment() && !segment.getTitles().isEmpty()) {
      handleTitleContenderFees(segment);
    }

    // Award fans to winners
    for (Wrestler winner : winners) {
      if (winner.getId() != null) {
        DiceBag wdb = new DiceBag(random, new int[] {6, 6});
        // for winners 2d6 + 3 + (quality bonus) fans
        long baseAward = (wdb.roll() + 3) * 1_000L + matchQualityBonus;
        long finalAward = (long) (baseAward * difficultyMultiplier);
        GeneralSecurityUtils.runAsAdmin(
                (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                    () -> wrestlerService.awardFans(winner.getId(), finalAward))
            .ifPresent(w -> log.debug("Awarded {} fans to winner {}", finalAward, w.getName()));
      }
    }

    // Award/deduct fans from losers
    for (Wrestler loser : losers) {
      if (loser.getId() != null) {
        DiceBag ldb = new DiceBag(random, new int[] {6});
        // for losers 1d6 - 4 + (quality bonus) fans. Can be negative
        long baseChange = (ldb.roll() - 4) * 1_000L + matchQualityBonus;
        // Apply multiplier only if it's a gain. If loss, maybe reduce penalty?
        // Or keep penalty standard? Let's apply multiplier to magnitude for now.
        // Actually, "Legendary" matches should probably reward more even for losing if quality is
        // high.
        // Let's apply multiplier to the whole thing.
        long finalChange = (long) (baseChange * difficultyMultiplier);

        GeneralSecurityUtils.runAsAdmin(
                (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                    () -> wrestlerService.awardFans(loser.getId(), finalChange))
            .ifPresent(
                w -> log.debug("Deducted/awarded {} fans to loser {}", finalChange, w.getName()));
      }
    }

    assignBumps(segment);
  }

  private void handlePromoRewards(Segment segment, int roll, double difficultyMultiplier) {
    int promoQualityBonus = calculatePromoQualityBonus(roll);

    // Assign fans to all participants
    for (Wrestler participant : segment.getWrestlers()) {
      if (participant.getId() != null) {
        long baseAward = promoQualityBonus * 1_000L;
        long finalAward = (long) (baseAward * difficultyMultiplier);
        GeneralSecurityUtils.runAsAdmin(
                (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                    () -> wrestlerService.awardFans(participant.getId(), finalAward))
            .ifPresent(
                w ->
                    log.debug(
                        "Awarded {} fans to wrestler {} during promo", finalAward, w.getName()));
      }
    }
  }

  private int calculateMatchQualityBonus(Segment segment, int roll) {
    int bonus = 0;
    if (11 <= roll && roll <= 15) {
      bonus += 1_000;
    } else if (16 <= roll && roll <= 18) {
      bonus += 3_000;
    } else if (roll == 19) {
      bonus += 5_000;
    } else if (roll == 20) {
      bonus += 10_000;
      // Trigger 5-Star Classic achievement for all participants
      segment
          .getWrestlers()
          .forEach(
              w -> {
                if (w.getAccount() != null) {
                  legacyService.unlockAchievement(w.getAccount(), "FIVE_STAR_CLASSIC");
                }
              });
    }
    return bonus;
  }

  private int calculatePromoQualityBonus(int roll) {
    int bonus = 0;
    if (2 <= roll && roll <= 3) {
      bonus += new DiceBag(random, new int[] {3}).roll();
    } else if (4 <= roll && roll <= 16) {
      bonus += new DiceBag(random, new int[] {6}).roll();
    } else if (17 <= roll && roll <= 19) {
      bonus += new DiceBag(random, new int[] {6, 6}).roll();
    } else if (roll == 20) {
      bonus += new DiceBag(random, new int[] {6, 6, 6}).roll();
    }
    return bonus;
  }

  private void handleTitleContenderFees(Segment segment) {
    for (Title title : segment.getTitles()) {
      List<Wrestler> currentChampions = title.getCurrentChampions();
      Long contenderEntryFee = titleService.getContenderEntryFee(title);

      for (Wrestler participant : segment.getWrestlers()) {
        if (!currentChampions.contains(participant)) {
          GeneralSecurityUtils.runAsAdmin(
                  (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                      () -> wrestlerService.awardFans(participant.getId(), -contenderEntryFee))
              .ifPresentOrElse(
                  w ->
                      log.info(
                          "Wrestler {} paid {} fans for contending in title segment {}",
                          w.getName(),
                          contenderEntryFee,
                          segment.getId()),
                  () ->
                      log.warn(
                          "Wrestler {} could not afford {} fans for contending in title segment {}",
                          participant.getName(),
                          contenderEntryFee,
                          segment.getId()));
        }
      }
    }
  }

  private void assignBumps(Segment segment) {
    for (SegmentRule rule : segment.getSegmentRules()) {
      switch (rule.getBumpAddition()) {
        case WINNERS:
          for (Wrestler winner : segment.getWinners()) {
            if (winner.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                          () -> wrestlerService.addBump(winner.getId()))
                  .ifPresent(w -> log.debug("Added bump to winner {}", w.getName()));
            }
          }
          break;
        case LOSERS:
          for (Wrestler loser : segment.getLosers()) {
            if (loser.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                          () -> wrestlerService.addBump(loser.getId()))
                  .ifPresent(w -> log.debug("Added bump to loser {}", w.getName()));
            }
          }
          break;
        case ALL:
          for (Wrestler participant : segment.getWrestlers()) {
            if (participant.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      (java.util.function.Supplier<java.util.Optional<Wrestler>>)
                          () -> wrestlerService.addBump(participant.getId()))
                  .ifPresent(w -> log.debug("Added bump to participant {}", w.getName()));
            }
          }
          break;
        case NONE:
        default:
          break;
      }
    }
  }
}

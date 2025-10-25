package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SegmentAdjudicationService {

  private final WrestlerService wrestlerService;
  private final Random random;

  @Autowired
  public SegmentAdjudicationService(WrestlerService wrestlerService) {
    this(wrestlerService, new Random());
  }

  public void adjudicateMatch(@NonNull Segment segment) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();
    if (!segment.getSegmentType().getName().equals("Promo")) {
      int matchQualityBonus = 0;
      if (11 <= roll && roll <= 15) {
        matchQualityBonus += 1_000;
      } else if (16 <= roll && roll <= 18) {
        matchQualityBonus += 3_000;
      } else if (roll == 19) {
        matchQualityBonus += 5_000;
      } else if (roll == 20) {
        matchQualityBonus += 10_000;
      }

      // Award fans to winners
      for (Wrestler winner : winners) {
        Long id = winner.getId();
        if (id != null) {
          DiceBag wdb = new DiceBag(random, new int[] {6, 6});
          // for winners 2d6 + 3 + (quality bonus) fans
          wrestlerService.awardFans(id, (wdb.roll() + 3) * 1_000L + matchQualityBonus);
        }
      }

      // Award fans from losers
      for (Wrestler loser : losers) {
        Long id = loser.getId();
        if (id != null) {
          DiceBag ldb = new DiceBag(random, new int[] {6});
          // for winners 1d6 + 3 + (quality bonus) fans
          wrestlerService.awardFans(loser.getId(), (ldb.roll() + 3) * 1_000L + matchQualityBonus);
        }
      }

      // Assign bumps to all participants
      for (Wrestler participant : segment.getWrestlers()) {
        Long id = participant.getId();
        if (id != null) {
          wrestlerService.addBump(id);
        }
      }
    } else {
      int promoQualityBonus = 0;
      if (2 <= roll && roll <= 3) {
        promoQualityBonus += new DiceBag(random, new int[] {3}).roll();
      } else if (4 <= roll && roll <= 16) {
        promoQualityBonus += new DiceBag(random, new int[] {6}).roll();
      } else if (17 <= roll && roll <= 19) {
        promoQualityBonus += new DiceBag(random, new int[] {6, 6}).roll();
      } else if (roll == 20) {
        promoQualityBonus += new DiceBag(random, new int[] {6, 6, 6}).roll();
      }
      // Assign bumps to all participants
      for (Wrestler participant : segment.getWrestlers()) {
        Long id = participant.getId();
        if (id != null) {
          wrestlerService.awardFans(id, promoQualityBonus * 1_000L);
        }
      }
    }
  }
}

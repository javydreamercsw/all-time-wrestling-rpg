package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchAdjudicationService {

  private final WrestlerService wrestlerService;

  public void adjudicateMatch(@NonNull Segment segment) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(20);
    int roll = d20.roll();
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
        DiceBag wdb = new DiceBag(6, 6);
        // for winners 2d6 + 3 + (quality bonus) fans
        wrestlerService.awardFans(id, (wdb.roll() + 3) * 1_000L + matchQualityBonus);
      }
    }

    // Award fans from losers
    for (Wrestler loser : losers) {
      Long id = loser.getId();
      if (id != null) {
        DiceBag ldb = new DiceBag(6);
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
  }
}

/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.ranking;

import com.github.javydreamercsw.management.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TierRecalculationService {

  private final WrestlerRepository wrestlerRepository;
  private final TierBoundaryService tierBoundaryService;

  public void recalculateTiers() {
    log.info("Starting tier recalculation...");
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    wrestlers.sort((w1, w2) -> w2.getFans().compareTo(w1.getFans()));
    int totalWrestlers = wrestlers.size();
    if (totalWrestlers == 0) {
      log.info("No wrestlers found to recalculate tiers.");
      return;
    }

    // Define percentile distribution for tiers
    Map<WrestlerTier, Double> tierDistribution = new EnumMap<>(WrestlerTier.class);
    tierDistribution.put(WrestlerTier.ICON, 0.05); // Top 5%
    tierDistribution.put(WrestlerTier.MAIN_EVENTER, 0.15); // Next 15%
    tierDistribution.put(WrestlerTier.MIDCARDER, 0.25); // Next 25%
    tierDistribution.put(WrestlerTier.CONTENDER, 0.25); // Next 25%
    tierDistribution.put(WrestlerTier.RISER, 0.20); // Next 20%
    // The rest are rookies
    tierDistribution.put(
        WrestlerTier.ROOKIE,
        1.0
            - tierDistribution.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum()); // Remaining

    // Determine fan count thresholds for each tier based on percentiles
    int currentWrestlerIndex = 0;
    long maxFansForNextLowerTier = Long.MAX_VALUE;

    WrestlerTier[] tiersHighToLow =
        new WrestlerTier[] {
          WrestlerTier.ICON,
          WrestlerTier.MAIN_EVENTER,
          WrestlerTier.MIDCARDER,
          WrestlerTier.CONTENDER,
          WrestlerTier.RISER,
          WrestlerTier.ROOKIE
        };

    for (WrestlerTier tier : tiersHighToLow) {
      int numWrestlersInTier;
      long minFans;

      if (tier == WrestlerTier.ROOKIE) {
        numWrestlersInTier = totalWrestlers - currentWrestlerIndex;
        minFans = 0; // Rookies start at 0
      } else {
        numWrestlersInTier = (int) Math.round(totalWrestlers * tierDistribution.get(tier));
        if (currentWrestlerIndex + numWrestlersInTier > totalWrestlers) {
          numWrestlersInTier = totalWrestlers - currentWrestlerIndex;
        }

        if (numWrestlersInTier > 0 && currentWrestlerIndex < totalWrestlers) {
          int boundaryIndex = currentWrestlerIndex + numWrestlersInTier - 1;
          if (boundaryIndex >= totalWrestlers) {
            boundaryIndex = totalWrestlers - 1;
          }
          if (tier == WrestlerTier.ICON) {
            minFans = wrestlers.get(boundaryIndex).getFans();
          } else {
            minFans = wrestlers.get(boundaryIndex).getFans() + 1;
          }
        } else {
          // If no wrestlers in this tier, set minFans based on the next lower tier's max
          minFans = maxFansForNextLowerTier > 0 ? maxFansForNextLowerTier + 1 : 0;
        }
      }

      TierBoundary boundary = tierBoundaryService.findByTier(tier).orElse(new TierBoundary());
      boundary.setTier(tier);
      boundary.setMinFans(minFans);
      boundary.setMaxFans(maxFansForNextLowerTier);
      boundary.setChallengeCost(Math.max(0, minFans / 100));
      boundary.setContenderEntryFee(Math.max(0, minFans / 200));
      tierBoundaryService.save(boundary);

      maxFansForNextLowerTier = minFans - 1;
      currentWrestlerIndex += numWrestlersInTier;
    }

    // Update wrestler tiers based on new boundaries
    for (Wrestler wrestler : wrestlers) {
      WrestlerTier newTier = tierBoundaryService.findTierForFans(wrestler.getFans());
      if (newTier != null) {
        if (wrestler.getTier() != newTier) {
          log.info(
              "Updating {}'s tier from {} to {}", wrestler.getName(), wrestler.getTier(), newTier);
          wrestler.setTier(newTier);
          wrestlerRepository.save(wrestler);
        }
      } else {
        log.warn(
            "Wrestler {} with {} fans does not match any tier!",
            wrestler.getName(),
            wrestler.getFans());
      }
    }
    log.info("Tier recalculation finished.");
  }
}

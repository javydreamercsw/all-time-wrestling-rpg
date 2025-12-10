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
    tierDistribution.put(WrestlerTier.ROOKIE, 0.10); // Bottom 10%

    // Ensure distribution sums to 1.0 (or close enough due to double precision)
    double sum = tierDistribution.values().stream().mapToDouble(Double::doubleValue).sum();

    if (Math.abs(sum - 1.0) > 0.001) {
      log.warn("Tier distribution percentages do not sum to 1.0. Adjusting for ROOKIE tier.");
      tierDistribution.put(
          WrestlerTier.ROOKIE, 1.0 - (sum - tierDistribution.get(WrestlerTier.ROOKIE)));
    }

    // Determine fan count thresholds for each tier based on percentiles
    // Iterate from highest tier (ICON) to lowest tier (ROOKIE) since wrestlers are sorted
    // descending
    Map<WrestlerTier, Long> minFanThresholds = new EnumMap<>(WrestlerTier.class);
    int currentWrestlerIndex = 0;

    // Process tiers from highest to lowest (ICON to ROOKIE)
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
      if (!tierDistribution.containsKey(tier)) continue;
      int numWrestlersInTier = (int) (totalWrestlers * tierDistribution.get(tier));
      if (numWrestlersInTier == 0 && tier != WrestlerTier.ROOKIE) {
        // If a tier gets 0 wrestlers, try to assign at least one if possible from next group
        numWrestlersInTier = 1;
      }

      if (currentWrestlerIndex + numWrestlersInTier > totalWrestlers) {
        numWrestlersInTier = totalWrestlers - currentWrestlerIndex;
      }

      if (numWrestlersInTier > 0) {
        // Calculate minFans based on comparison semantics
        int endIndex = currentWrestlerIndex + numWrestlersInTier - 1;
        if (tier == WrestlerTier.ROOKIE) {
          // ROOKIE uses >=, so minFans is the actual minimum fans of wrestlers in the tier
          minFanThresholds.put(tier, wrestlers.get(endIndex).getFans());
        } else {
          // Other tiers use >, so minFans should be one less than minimum fans in the tier
          // The minimum fans in the tier is the last wrestler's fans (endIndex)
          minFanThresholds.put(tier, wrestlers.get(endIndex).getFans() - 1);
        }
      } else {
        // Handle cases where no wrestlers fall into this tier initially
        minFanThresholds.put(tier, 0L);
      }
      currentWrestlerIndex += numWrestlersInTier;
    }

    // Create/Update TierBoundary objects from highest tier to lowest
    Map<WrestlerTier, TierBoundary> newBoundaries = new EnumMap<>(WrestlerTier.class);
    long maxFansForNextLowerTier = Long.MAX_VALUE;
    for (int i = WrestlerTier.values().length - 1; i >= 0; i--) {
      WrestlerTier tier = WrestlerTier.values()[i];
      long minFans = minFanThresholds.getOrDefault(tier, 0L);
      TierBoundary boundary = tierBoundaryService.findByTier(tier).orElse(new TierBoundary());
      boundary.setTier(tier);
      boundary.setMinFans(minFans);
      boundary.setMaxFans(maxFansForNextLowerTier); // Set maxFans for current tier

      // Calculate fees (1% of minFans for challenge, 0.5% for contender entry)
      boundary.setChallengeCost(Math.max(0, minFans / 100));
      boundary.setContenderEntryFee(Math.max(0, minFans / 200));
      tierBoundaryService.save(boundary);
      newBoundaries.put(tier, boundary);

      // Set maxFans for the next lower tier
      if (tier == WrestlerTier.RISER) {
        // RISER uses > for minFans, so ROOKIE can include wrestlers with exactly RISER.minFans
        maxFansForNextLowerTier = minFans;
      } else {
        // Other tiers: subtract 1 to avoid overlap
        maxFansForNextLowerTier = minFans - 1;
      }
    }

    // Update wrestler tiers based on new boundaries
    for (Wrestler wrestler : wrestlers) {
      WrestlerTier oldTier = wrestler.getTier();
      WrestlerTier newTier = null;
      for (WrestlerTier tier : WrestlerTier.values()) {
        TierBoundary boundary = newBoundaries.get(tier);
        boolean matches;
        if (tier == WrestlerTier.ROOKIE) {
          // ROOKIE is the lowest tier, use >= for minFans
          matches =
              wrestler.getFans() >= boundary.getMinFans()
                  && wrestler.getFans() <= boundary.getMaxFans();
        } else {
          // Other tiers use > for minFans (exclusive lower bound)
          matches =
              wrestler.getFans() > boundary.getMinFans()
                  && wrestler.getFans() <= boundary.getMaxFans();
        }
        if (matches) {
          newTier = tier;
          break;
        }
      }

      if (newTier != null) {
        wrestler.setTier(newTier);
        wrestlerRepository.save(wrestler);
        if (oldTier != newTier) {
          log.info(
              "Wrestler {}'s tier changed from {} to {}", wrestler.getName(), oldTier, newTier);
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

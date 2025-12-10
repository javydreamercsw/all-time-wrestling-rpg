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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierRecalculationServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TierBoundaryService tierBoundaryService;

  @InjectMocks private TierRecalculationService tierRecalculationService;

  @Captor private ArgumentCaptor<TierBoundary> tierBoundaryCaptor;
  @Captor private ArgumentCaptor<Wrestler> wrestlerCaptor;

  private List<Wrestler> wrestlers;

  @BeforeEach
  void setUp() {
    wrestlers = new ArrayList<>();
    // Create 20 wrestlers with varying fan counts (20000, 19000, ..., 1000)
    for (int i = 0; i < 20; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      w.setFans(1000L * (20 - i)); // Wrestler 0 has 20000, Wrestler 19 has 1000
      wrestlers.add(w);
    }
  }

  @Test
  void testRecalculateTiersWithNewBoundaries() {
    when(wrestlerRepository.findAll()).thenReturn(wrestlers);
    when(tierBoundaryService.findByTier(any(WrestlerTier.class))).thenReturn(Optional.empty());

    tierRecalculationService.recalculateTiers();

    // Verify TierBoundaryService.save is called for each tier
    verify(tierBoundaryService, times(WrestlerTier.values().length))
        .save(tierBoundaryCaptor.capture());
    List<TierBoundary> capturedBoundaries = tierBoundaryCaptor.getAllValues();
    Map<WrestlerTier, TierBoundary> boundariesMap =
        capturedBoundaries.stream().collect(Collectors.toMap(TierBoundary::getTier, b -> b));

    // Assert boundaries and fees based on 20 wrestlers and percentile distribution
    // ICON (5% = 1): Wrestler 0 (20000 fans)
    TierBoundary iconBoundary = boundariesMap.get(WrestlerTier.ICON);
    assertEquals(19999L, iconBoundary.getMinFans()); // One less than minimum in tier (uses >)
    assertEquals(Long.MAX_VALUE, iconBoundary.getMaxFans());
    assertEquals(199L, iconBoundary.getChallengeCost()); // 1% of 19999
    assertEquals(99L, iconBoundary.getContenderEntryFee()); // 0.5% of 19999

    // MAIN_EVENTER (15% = 3): Wrestlers 1-3 (19000-17000 fans)
    TierBoundary mainEventerBoundary = boundariesMap.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(16999L, mainEventerBoundary.getMinFans()); // One less than 17000 (wrestler 3)
    assertEquals(19998L, mainEventerBoundary.getMaxFans()); // minFans of ICON - 1
    assertEquals(169L, mainEventerBoundary.getChallengeCost());
    assertEquals(84L, mainEventerBoundary.getContenderEntryFee());

    // MIDCARDER (25% = 5): Wrestlers 4-8 (16000-12000 fans)
    TierBoundary midcarderBoundary = boundariesMap.get(WrestlerTier.MIDCARDER);
    assertEquals(11999L, midcarderBoundary.getMinFans()); // One less than 12000 (wrestler 8)
    assertEquals(16998L, midcarderBoundary.getMaxFans()); // minFans of MAIN_EVENTER - 1
    assertEquals(119L, midcarderBoundary.getChallengeCost());
    assertEquals(59L, midcarderBoundary.getContenderEntryFee());

    // CONTENDER (25% = 5): Wrestlers 9-13 (11000-7000 fans)
    TierBoundary contenderBoundary = boundariesMap.get(WrestlerTier.CONTENDER);
    assertEquals(6999L, contenderBoundary.getMinFans()); // One less than 7000 (wrestler 13)
    assertEquals(11998L, contenderBoundary.getMaxFans()); // minFans of MIDCARDER - 1
    assertEquals(69L, contenderBoundary.getChallengeCost());
    assertEquals(34L, contenderBoundary.getContenderEntryFee());

    // RISER (20% = 4): Wrestlers 14-17 (6000-3000 fans)
    TierBoundary riserBoundary = boundariesMap.get(WrestlerTier.RISER);
    assertEquals(2999L, riserBoundary.getMinFans()); // One less than 3000 (wrestler 17)
    assertEquals(6998L, riserBoundary.getMaxFans()); // minFans of CONTENDER - 1
    assertEquals(29L, riserBoundary.getChallengeCost());
    assertEquals(14L, riserBoundary.getContenderEntryFee());

    // ROOKIE (10% = 2): Wrestlers 18-19 (2000-1000 fans)
    TierBoundary rookieBoundary = boundariesMap.get(WrestlerTier.ROOKIE);
    assertEquals(1000L, rookieBoundary.getMinFans()); // Wrestler 19 has 1000 fans (uses >=)
    assertEquals(2999L, rookieBoundary.getMaxFans()); // Same as RISER minFans (RISER uses >)
    assertEquals(10L, rookieBoundary.getChallengeCost());
    assertEquals(5L, rookieBoundary.getContenderEntryFee());

    // Verify wrestler tiers are updated
    verify(wrestlerRepository, times(wrestlers.size())).save(wrestlerCaptor.capture());
    List<Wrestler> updatedWrestlers = wrestlerCaptor.getAllValues();
    Map<Long, Wrestler> updatedWrestlersMap =
        updatedWrestlers.stream().collect(Collectors.toMap(Wrestler::getId, w -> w));

    assertEquals(WrestlerTier.ICON, updatedWrestlersMap.get(wrestlers.get(0).getId()).getTier());
    assertEquals(
        WrestlerTier.MAIN_EVENTER, updatedWrestlersMap.get(wrestlers.get(1).getId()).getTier());
    assertEquals(
        WrestlerTier.MAIN_EVENTER, updatedWrestlersMap.get(wrestlers.get(3).getId()).getTier());
    assertEquals(
        WrestlerTier.MIDCARDER, updatedWrestlersMap.get(wrestlers.get(4).getId()).getTier());
    assertEquals(
        WrestlerTier.CONTENDER, updatedWrestlersMap.get(wrestlers.get(9).getId()).getTier());
    assertEquals(WrestlerTier.RISER, updatedWrestlersMap.get(wrestlers.get(14).getId()).getTier());
    assertEquals(WrestlerTier.ROOKIE, updatedWrestlersMap.get(wrestlers.get(18).getId()).getTier());
    assertEquals(WrestlerTier.ROOKIE, updatedWrestlersMap.get(wrestlers.get(19).getId()).getTier());
  }

  @Test
  void testRecalculateTiersWithExistingBoundaries() {
    // Test case where boundaries already exist and are updated
    for (WrestlerTier tier : WrestlerTier.values()) {
      TierBoundary existingBoundary = new TierBoundary();
      existingBoundary.setTier(tier);
      existingBoundary.setMinFans(0L); // Old value
      when(tierBoundaryService.findByTier(tier)).thenReturn(Optional.of(existingBoundary));
    }

    when(wrestlerRepository.findAll()).thenReturn(wrestlers);

    tierRecalculationService.recalculateTiers();

    verify(tierBoundaryService, times(WrestlerTier.values().length))
        .save(tierBoundaryCaptor.capture());
    List<TierBoundary> capturedBoundaries = tierBoundaryCaptor.getAllValues();
    Map<WrestlerTier, TierBoundary> boundariesMap =
        capturedBoundaries.stream().collect(Collectors.toMap(TierBoundary::getTier, b -> b));

    // Verify that the minFans for ICON is updated to the new calculated value
    assertEquals(19999L, boundariesMap.get(WrestlerTier.ICON).getMinFans());
  }

  @Test
  void testRecalculateTiersNoWrestlers() {
    when(wrestlerRepository.findAll()).thenReturn(new ArrayList<>());
    tierRecalculationService.recalculateTiers();
    verify(tierBoundaryService, times(0)).save(any(TierBoundary.class));
    verify(wrestlerRepository, times(0)).save(any(Wrestler.class));
  }
}

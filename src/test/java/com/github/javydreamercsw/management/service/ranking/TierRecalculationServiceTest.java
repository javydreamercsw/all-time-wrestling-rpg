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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.ArrayList;
import java.util.EnumMap;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TierRecalculationServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TierBoundaryService tierBoundaryService;

  @InjectMocks private TierRecalculationService tierRecalculationService;

  @Captor private ArgumentCaptor<TierBoundary> tierBoundaryCaptor;
  @Captor private ArgumentCaptor<Wrestler> wrestlerCaptor;

  private List<Wrestler> wrestlers;

  private Map<WrestlerTier, TierBoundary> inMemoryTierBoundaries;

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

    inMemoryTierBoundaries = new EnumMap<>(WrestlerTier.class);

    // Configure the mocked TierBoundaryService to use the in-memory map
    when(tierBoundaryService.save(any(TierBoundary.class)))
        .thenAnswer(
            invocation -> {
              TierBoundary boundary = invocation.getArgument(0);
              inMemoryTierBoundaries.put(boundary.getTier(), boundary);
              return boundary;
            });

    when(tierBoundaryService.findAll())
        .thenAnswer(invocation -> new ArrayList<>(inMemoryTierBoundaries.values()));
    when(tierBoundaryService.findByTier(any(WrestlerTier.class)))
        .thenAnswer(
            invocation -> {
              WrestlerTier tier = invocation.getArgument(0);
              return Optional.ofNullable(inMemoryTierBoundaries.get(tier));
            });

    when(tierBoundaryService.findTierForFans(anyLong()))
        .thenAnswer(
            invocation -> {
              long fans = invocation.getArgument(0);
              return inMemoryTierBoundaries.values().stream()
                  .sorted((b1, b2) -> b2.getMinFans().compareTo(b1.getMinFans()))
                  .filter(b -> fans >= b.getMinFans() && fans <= b.getMaxFans())
                  .map(TierBoundary::getTier)
                  .findFirst()
                  .orElse(null);
            });

    when(wrestlerRepository.save(any(Wrestler.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void testRecalculateTiersWithNewBoundaries() {
    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlers));

    // Use the in-memory boundaries map instead of captor
    Map<WrestlerTier, TierBoundary> boundariesMap = new EnumMap<>(inMemoryTierBoundaries);

    // Verify all tier boundaries were saved
    verify(tierBoundaryService, times(WrestlerTier.values().length)).save(any(TierBoundary.class));

    // Assert boundaries and fees based on 20 wrestlers and percentile distribution
    // ICON (5% = 1): Wrestler 0 (20000 fans)
    TierBoundary iconBoundary = boundariesMap.get(WrestlerTier.ICON);
    assertEquals(20000L, iconBoundary.getMinFans()); // Aligned with actual lowest in tier
    assertEquals(Long.MAX_VALUE, iconBoundary.getMaxFans());
    assertEquals(200L, iconBoundary.getChallengeCost()); // 1% of 20000
    assertEquals(100L, iconBoundary.getContenderEntryFee()); // 0.5% of 20000

    // MAIN_EVENTER (15% = 3): Wrestlers 1-3 (19000-17000 fans)
    TierBoundary mainEventerBoundary = boundariesMap.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(
        17001L, mainEventerBoundary.getMinFans()); // Aligned with actual lowest in tier + 1
    assertEquals(19999L, mainEventerBoundary.getMaxFans()); // minFans of ICON - 1
    assertEquals(170L, mainEventerBoundary.getChallengeCost()); // 1% of 17000
    assertEquals(85L, mainEventerBoundary.getContenderEntryFee()); // 0.5% of 17000

    // MIDCARDER (25% = 5): Wrestlers 4-8 (16000-12000 fans)
    TierBoundary midcarderBoundary = boundariesMap.get(WrestlerTier.MIDCARDER);
    assertEquals(12001L, midcarderBoundary.getMinFans()); // lowest + 1
    assertEquals(17000L, midcarderBoundary.getMaxFans()); // minFans of MAIN_EVENTER - 1
    assertEquals(120L, midcarderBoundary.getChallengeCost()); // 1% of 12000
    assertEquals(60L, midcarderBoundary.getContenderEntryFee()); // 0.5% of 12000

    // CONTENDER (25% = 5): Wrestlers 9-13 (11000-7000 fans)
    TierBoundary contenderBoundary = boundariesMap.get(WrestlerTier.CONTENDER);
    assertEquals(7001L, contenderBoundary.getMinFans()); // lowest + 1
    assertEquals(12000L, contenderBoundary.getMaxFans()); // minFans of MIDCARDER - 1
    assertEquals(70L, contenderBoundary.getChallengeCost()); // 1% of 7000
    assertEquals(35L, contenderBoundary.getContenderEntryFee()); // 0.5% of 7000

    // RISER (20% = 4): Wrestlers 14-17 (6000-3000 fans)
    TierBoundary riserBoundary = boundariesMap.get(WrestlerTier.RISER);
    assertEquals(3001L, riserBoundary.getMinFans()); // lowest + 1
    assertEquals(7000L, riserBoundary.getMaxFans()); // minFans of CONTENDER - 1
    assertEquals(30L, riserBoundary.getChallengeCost()); // 1% of 3000
    assertEquals(15L, riserBoundary.getContenderEntryFee()); // 0.5% of 3000

    // ROOKIE (10% = 2): Wrestlers 18-19 (2000-1000 fans)
    TierBoundary rookieBoundary = boundariesMap.get(WrestlerTier.ROOKIE);
    assertEquals(0L, rookieBoundary.getMinFans()); // Rookies start at 0
    assertEquals(3000L, rookieBoundary.getMaxFans()); // RISER minFans - 1
    verify(wrestlerRepository, times(wrestlers.size())).save(wrestlerCaptor.capture());
    List<Wrestler> updatedWrestlers = wrestlerCaptor.getAllValues();
    Map<Long, Wrestler> updatedWrestlersMap =
        updatedWrestlers.stream().collect(Collectors.toMap(Wrestler::getId, w -> w));

    assertEquals(WrestlerTier.ICON, updatedWrestlersMap.get(wrestlers.get(0).getId()).getTier());
    assertEquals(
        WrestlerTier.MAIN_EVENTER, updatedWrestlersMap.get(wrestlers.get(1).getId()).getTier());
    assertEquals(
        WrestlerTier.MAIN_EVENTER, updatedWrestlersMap.get(wrestlers.get(2).getId()).getTier());
    assertEquals(
        WrestlerTier.MIDCARDER, updatedWrestlersMap.get(wrestlers.get(3).getId()).getTier());
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
      inMemoryTierBoundaries.put(tier, existingBoundary);
    }

    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlers));

    // Verify all tier boundaries were saved
    verify(tierBoundaryService, times(WrestlerTier.values().length)).save(any(TierBoundary.class));

    // Use the in-memory boundaries map
    Map<WrestlerTier, TierBoundary> boundariesMap = new EnumMap<>(inMemoryTierBoundaries);

    // Assert boundaries and fees based on 20 wrestlers and percentile distribution
    // ICON (5% = 1): Wrestler 0 (20000 fans)
    TierBoundary iconBoundary = boundariesMap.get(WrestlerTier.ICON);
    assertEquals(20000L, iconBoundary.getMinFans()); // Aligned with actual lowest in tier
    assertEquals(Long.MAX_VALUE, iconBoundary.getMaxFans());
    assertEquals(200L, iconBoundary.getChallengeCost()); // 1% of 20000
    assertEquals(100L, iconBoundary.getContenderEntryFee()); // 0.5% of 20000

    // MAIN_EVENTER (15% = 3): Wrestlers 1-3 (19000-17000 fans)
    TierBoundary mainEventerBoundary = boundariesMap.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(
        17001L, mainEventerBoundary.getMinFans()); // Aligned with actual lowest in tier + 1
    assertEquals(19999L, mainEventerBoundary.getMaxFans()); // minFans of ICON - 1
    assertEquals(170L, mainEventerBoundary.getChallengeCost()); // 1% of 17000
    assertEquals(85L, mainEventerBoundary.getContenderEntryFee()); // 0.5% of 17000

    // MIDCARDER (25% = 5): Wrestlers 4-8 (16000-12000 fans)
    TierBoundary midcarderBoundary = boundariesMap.get(WrestlerTier.MIDCARDER);
    assertEquals(12001L, midcarderBoundary.getMinFans()); // lowest + 1
    assertEquals(17000L, midcarderBoundary.getMaxFans()); // minFans of MAIN_EVENTER - 1
    assertEquals(120L, midcarderBoundary.getChallengeCost()); // 1% of 12000
    assertEquals(60L, midcarderBoundary.getContenderEntryFee()); // 0.5% of 12000

    // CONTENDER (25% = 5): Wrestlers 9-13 (11000-7000 fans)
    TierBoundary contenderBoundary = boundariesMap.get(WrestlerTier.CONTENDER);
    assertEquals(7001L, contenderBoundary.getMinFans()); // lowest + 1
    assertEquals(12000L, contenderBoundary.getMaxFans()); // minFans of MIDCARDER - 1
    assertEquals(70L, contenderBoundary.getChallengeCost()); // 1% of 7000
    assertEquals(35L, contenderBoundary.getContenderEntryFee()); // 0.5% of 7000

    // RISER (20% = 4): Wrestlers 14-17 (6000-3000 fans)
    TierBoundary riserBoundary = boundariesMap.get(WrestlerTier.RISER);
    assertEquals(3001L, riserBoundary.getMinFans()); // lowest + 1
    assertEquals(7000L, riserBoundary.getMaxFans()); // minFans of CONTENDER - 1
    assertEquals(30L, riserBoundary.getChallengeCost()); // 1% of 3000
    assertEquals(15L, riserBoundary.getContenderEntryFee()); // 0.5% of 3000

    // ROOKIE (10% = 2): Wrestlers 18-19 (2000-1000 fans)
    TierBoundary rookieBoundary = boundariesMap.get(WrestlerTier.ROOKIE);
    assertEquals(0L, rookieBoundary.getMinFans()); // Rookies start at 0
    assertEquals(3000L, rookieBoundary.getMaxFans()); // RISER minFans - 1
  }

  @Test
  void testRecalculateTiersNoWrestlers() {
    tierRecalculationService.recalculateRanking(new ArrayList<>());
    verify(tierBoundaryService, times(0)).save(any(TierBoundary.class));
    verify(wrestlerRepository, times(0)).save(any(Wrestler.class));
  }
}

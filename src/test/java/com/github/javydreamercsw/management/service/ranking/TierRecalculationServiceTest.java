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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
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

  private Map<Gender, Map<WrestlerTier, TierBoundary>> inMemoryTierBoundaries;

  @BeforeEach
  void setUp() {
    wrestlers = new ArrayList<>();
    // Create 20 wrestlers with varying fan counts (20000, 19000, ..., 1000)
    for (int i = 0; i < 20; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      w.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
      w.setFans(1000L * (20 - i)); // Wrestler 0 has 20000, Wrestler 19 has 1000
      wrestlers.add(w);
    }

    inMemoryTierBoundaries = new EnumMap<>(Gender.class);
    inMemoryTierBoundaries.put(Gender.MALE, new EnumMap<>(WrestlerTier.class));
    inMemoryTierBoundaries.put(Gender.FEMALE, new EnumMap<>(WrestlerTier.class));

    // Configure the mocked TierBoundaryService to use the in-memory map
    when(tierBoundaryService.save(any(TierBoundary.class)))
        .thenAnswer(
            invocation -> {
              TierBoundary boundary = invocation.getArgument(0);
              inMemoryTierBoundaries.get(boundary.getGender()).put(boundary.getTier(), boundary);
              return boundary;
            });

    when(tierBoundaryService.findAll())
        .thenAnswer(
            invocation ->
                inMemoryTierBoundaries.values().stream()
                    .flatMap(m -> m.values().stream())
                    .collect(Collectors.toList()));

    when(tierBoundaryService.findAllByGender(any(Gender.class)))
        .thenAnswer(
            invocation -> {
              Gender gender = invocation.getArgument(0);
              return new ArrayList<>(inMemoryTierBoundaries.get(gender).values());
            });

    when(tierBoundaryService.findByTierAndGender(any(WrestlerTier.class), any(Gender.class)))
        .thenAnswer(
            invocation -> {
              WrestlerTier tier = invocation.getArgument(0);
              Gender gender = invocation.getArgument(1);
              return Optional.ofNullable(inMemoryTierBoundaries.get(gender).get(tier));
            });

    when(tierBoundaryService.findTierForFans(anyLong(), any(Gender.class)))
        .thenAnswer(
            invocation -> {
              long fans = invocation.getArgument(0);
              Gender gender = invocation.getArgument(1);
              return inMemoryTierBoundaries.get(gender).values().stream()
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

    // Male wrestlers (10 wrestlers)
    Map<WrestlerTier, TierBoundary> maleBoundaries = inMemoryTierBoundaries.get(Gender.MALE);
    assertEquals(
        WrestlerTier.values().length,
        maleBoundaries.size(),
        "Should have boundaries for all tiers for males.");

    // ICON (5% = 1 of 10 -> 1): Wrestler 0 (20000 fans)
    TierBoundary maleIconBoundary = maleBoundaries.get(WrestlerTier.ICON);
    assertEquals(20000L, maleIconBoundary.getMinFans());
    assertEquals(Long.MAX_VALUE, maleIconBoundary.getMaxFans());

    // Female wrestlers (10 wrestlers)
    Map<WrestlerTier, TierBoundary> femaleBoundaries = inMemoryTierBoundaries.get(Gender.FEMALE);
    assertEquals(
        WrestlerTier.values().length,
        femaleBoundaries.size(),
        "Should have boundaries for all tiers for females.");

    // ICON (5% = 1 of 10 -> 1): Wrestler 1 (19000 fans)
    TierBoundary femaleIconBoundary = femaleBoundaries.get(WrestlerTier.ICON);
    assertEquals(19000L, femaleIconBoundary.getMinFans());
    assertEquals(Long.MAX_VALUE, femaleIconBoundary.getMaxFans());

    verify(wrestlerRepository, times(wrestlers.size())).save(wrestlerCaptor.capture());
  }

  @Test
  void testRecalculateTiersWithExistingBoundaries() {
    // Test case where boundaries already exist and are updated
    for (Gender gender : Gender.values()) {
      for (WrestlerTier tier : WrestlerTier.values()) {
        TierBoundary existingBoundary = new TierBoundary();
        existingBoundary.setTier(tier);
        existingBoundary.setGender(gender);
        existingBoundary.setMinFans(0L); // Old value
        inMemoryTierBoundaries.get(gender).put(tier, existingBoundary);
      }
    }

    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlers));

    // Verify all tier boundaries were saved
    verify(tierBoundaryService, times(WrestlerTier.values().length * Gender.values().length))
        .save(any(TierBoundary.class));
  }

  @Test
  void testRecalculateTiersNoWrestlers() {
    tierRecalculationService.recalculateRanking(new ArrayList<>());
    verify(tierBoundaryService, times(0)).save(any(TierBoundary.class));
    verify(wrestlerRepository, times(0)).save(any(Wrestler.class));
  }
}

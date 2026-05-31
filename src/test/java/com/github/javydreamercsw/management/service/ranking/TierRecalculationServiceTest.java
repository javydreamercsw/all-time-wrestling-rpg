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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private TierBoundaryService tierBoundaryService;

  @InjectMocks private TierRecalculationService tierRecalculationService;

  @Captor private ArgumentCaptor<TierBoundary> tierBoundaryCaptor;
  @Captor private ArgumentCaptor<WrestlerState> wrestlerStateCaptor;

  private List<WrestlerState> wrestlerStates;
  private Universe universe;

  private Map<Gender, Map<WrestlerTier, TierBoundary>> inMemoryTierBoundaries;

  @BeforeEach
  public void setUp() {
    universe = Universe.builder().name("Test Universe").build();
    wrestlerStates = new ArrayList<>();
    // Create 20 wrestlers with varying fan counts (20000, 19000, ..., 1000)
    for (int i = 0; i < 20; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      w.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);

      WrestlerState s =
          WrestlerState.builder()
              .wrestler(w)
              .universe(universe)
              .fans(1000L * (20 - i)) // Wrestler 0 has 20000, Wrestler 19 has 1000
              .tier(WrestlerTier.ROOKIE)
              .build();
      wrestlerStates.add(s);
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

    when(wrestlerStateRepository.save(any(WrestlerState.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void testRecalculateTiersWithNewBoundaries() {
    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlerStates));

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

    // MAIN_EVENTER (15% = 1.5 -> 2):
    // currentWrestlerIndex was 1. numWrestlersInTier = Math.round(10 * 0.15) = 2.
    // boundaryIndex = 1 + 2 - 1 = 2. Wrestler 2 (male) has 16000 fans.
    TierBoundary maleMeBoundary = maleBoundaries.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(16000L, maleMeBoundary.getMinFans());

    // Female wrestlers (10 wrestlers)
    Map<WrestlerTier, TierBoundary> femaleBoundaries = inMemoryTierBoundaries.get(Gender.FEMALE);
    assertEquals(
        WrestlerTier.values().length,
        femaleBoundaries.size(),
        "Should have boundaries for all tiers for females.");

    // ICON (5% = 0.5 -> 1): Wrestler 1 (19000 fans)
    TierBoundary femaleIconBoundary = femaleBoundaries.get(WrestlerTier.ICON);
    assertEquals(19000L, femaleIconBoundary.getMinFans());
    assertEquals(Long.MAX_VALUE, femaleIconBoundary.getMaxFans());

    // MAIN_EVENTER (15% = 1.5 -> 2): Wrestler 5 (female) has 15000 fans.
    TierBoundary femaleMeBoundary = femaleBoundaries.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(15000L, femaleMeBoundary.getMinFans());

    verify(wrestlerStateRepository, times(20)).save(wrestlerStateCaptor.capture());
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

    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlerStates));

    // Verify all tier boundaries were saved
    verify(tierBoundaryService, times(WrestlerTier.values().length * Gender.values().length))
        .save(any(TierBoundary.class));
  }

  @Test
  void testRecalculateTiersNoWrestlers() {
    tierRecalculationService.recalculateRanking(new ArrayList<>());
    verify(tierBoundaryService, times(0)).save(any(TierBoundary.class));
    verify(wrestlerStateRepository, times(0)).save(any(WrestlerState.class));
  }

  @Test
  void testRecalculateTiersWithZeroFans() {
    List<WrestlerState> zeroFanWrestlerStates = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      w.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);

      WrestlerState s =
          WrestlerState.builder()
              .wrestler(w)
              .universe(universe)
              .fans(0L)
              .tier(WrestlerTier.ROOKIE)
              .build();
      zeroFanWrestlerStates.add(s);
    }

    tierRecalculationService.recalculateRanking(new ArrayList<>(zeroFanWrestlerStates));

    Map<WrestlerTier, TierBoundary> maleBoundaries = inMemoryTierBoundaries.get(Gender.MALE);
    assertEquals(
        WrestlerTier.values().length,
        maleBoundaries.size(),
        "Should have boundaries for all tiers for males.");

    for (WrestlerTier tier : WrestlerTier.values()) {
      assertEquals(
          tier.getMinFans(),
          maleBoundaries.get(tier).getMinFans(),
          "Min fans for " + tier + " should be the default value.");
    }

    Map<WrestlerTier, TierBoundary> femaleBoundaries = inMemoryTierBoundaries.get(Gender.FEMALE);
    assertEquals(
        WrestlerTier.values().length,
        femaleBoundaries.size(),
        "Should have boundaries for all tiers for females.");

    for (WrestlerTier tier : WrestlerTier.values()) {
      assertEquals(
          tier.getMinFans(),
          femaleBoundaries.get(tier).getMinFans(),
          "Min fans for " + tier + " should be the default value.");
    }
  }

  @Test
  void testRecalculateTiersSmallRoster() {
    // 9 female wrestlers
    List<WrestlerState> smallRoster = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Female Wrestler " + i);
      w.setGender(Gender.FEMALE);

      WrestlerState s =
          WrestlerState.builder()
              .wrestler(w)
              .universe(universe)
              .fans(100L * (10 - i)) // 1000, 900, ..., 200
              .tier(WrestlerTier.ROOKIE)
              .build();
      smallRoster.add(s);
    }

    tierRecalculationService.recalculateRanking(new ArrayList<>(smallRoster));

    Map<WrestlerTier, TierBoundary> femaleBoundaries = inMemoryTierBoundaries.get(Gender.FEMALE);

    // With 9 wrestlers, 5% is 0.45, but our new logic ensures at least 1.
    TierBoundary femaleIconBoundary = femaleBoundaries.get(WrestlerTier.ICON);
    assertEquals(1000L, femaleIconBoundary.getMinFans());

    // 15% of 9 is 1.35, which rounds to 1.
    TierBoundary femaleMeBoundary = femaleBoundaries.get(WrestlerTier.MAIN_EVENTER);
    assertEquals(900L, femaleMeBoundary.getMinFans());
  }

  @Test
  void testRecalculateSingleWrestlerTier() {
    // Setup boundaries
    TierBoundary boundary = new TierBoundary();
    boundary.setTier(WrestlerTier.ICON);
    boundary.setGender(Gender.MALE);
    boundary.setMinFans(50000L);
    boundary.setMaxFans(Long.MAX_VALUE);
    inMemoryTierBoundaries.get(Gender.MALE).put(WrestlerTier.ICON, boundary);

    Wrestler w = new Wrestler();
    w.setGender(Gender.MALE);

    WrestlerState wrestler =
        WrestlerState.builder().wrestler(w).fans(60000L).tier(WrestlerTier.ROOKIE).build();

    tierRecalculationService.recalculateTier(wrestler);

    assertEquals(WrestlerTier.ICON, wrestler.getTier());
  }
}

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
package com.github.javydreamercsw.management.service.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentOutcomeServiceHomeFieldTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private InjuryService injuryService;

  private SegmentOutcomeService service;

  @BeforeEach
  void setUp() {
    service =
        new SegmentOutcomeService(
            wrestlerRepository, wrestlerService, injuryService, new Random(42));

    Wrestler homeDbWrestler = new Wrestler();
    homeDbWrestler.setId(1L);
    homeDbWrestler.setName("Kazuchika Okada");
    WrestlerState homeState = new WrestlerState();
    homeState.setWrestler(homeDbWrestler);
    homeDbWrestler.getWrestlerStates().add(homeState);

    homeState.setFans(10_000L);
    homeState.setTier(WrestlerTier.MIDCARDER);
    homeState.setBumps(0);
    homeDbWrestler.setDecks(Collections.emptySet());

    Wrestler awayDbWrestler = new Wrestler();
    awayDbWrestler.setId(2L);
    awayDbWrestler.setName("John Doe");
    WrestlerState awayState = new WrestlerState();
    awayState.setWrestler(awayDbWrestler);
    awayDbWrestler.getWrestlerStates().add(awayState);

    awayState.setFans(10_000L);
    awayState.setTier(WrestlerTier.MIDCARDER);
    awayState.setBumps(0);
    awayDbWrestler.setDecks(Collections.emptySet());

    when(wrestlerRepository.findByName("Kazuchika Okada")).thenReturn(Optional.of(homeDbWrestler));
    when(wrestlerRepository.findByName("John Doe")).thenReturn(Optional.of(awayDbWrestler));

    when(wrestlerService.getOrCreateState(eq(1L), anyLong())).thenReturn(homeState);
    when(wrestlerService.getOrCreateState(eq(2L), anyLong())).thenReturn(awayState);
  }

  private WrestlerContext makeContext(
      @NonNull final String name, @NonNull final String hailingFrom) {
    WrestlerContext ctx = new SegmentNarrationService.WrestlerContext();
    ctx.setName(name);
    ctx.setHailingFrom(hailingFrom);
    ctx.setHealth(100);
    ctx.setStamina(100);
    return ctx;
  }

  private VenueContext makeVenue(@NonNull final String location) {
    VenueContext venue = new SegmentNarrationService.VenueContext();
    venue.setName("Tokyo Dome");
    venue.setLocation(location);
    return venue;
  }

  private SegmentNarrationService.SegmentNarrationContext makeContext(
      @NonNull final List<WrestlerContext> wrestlers, final VenueContext venue) {
    SegmentNarrationService.SegmentNarrationContext ctx =
        new SegmentNarrationService.SegmentNarrationContext();
    SegmentNarrationService.SegmentTypeContext st =
        new SegmentNarrationService.SegmentTypeContext();
    st.setSegmentType("One on One");
    ctx.setSegmentType(st);
    ctx.setWrestlers(wrestlers);
    ctx.setVenue(venue);
    return ctx;
  }

  @Test
  void testHomeFieldBoostIncreasesWinProbability() {
    // Okada hails from Japan, venue is in Japan — home boost applies
    // Doe hails from USA — no boost
    WrestlerContext homeCtx = makeContext("Kazuchika Okada", "Japan");
    WrestlerContext awayCtx = makeContext("John Doe", "USA");
    VenueContext venue = makeVenue("Tokyo, Japan");

    // Run many trials to confirm Okada wins more often with home boost
    int homeWins = 0;
    int trials = 1000;
    for (int i = 0; i < trials; i++) {
      SegmentNarrationService.SegmentNarrationContext ctx =
          makeContext(List.of(homeCtx, awayCtx), venue);
      service.determineOutcomeIfNeeded(ctx);
      if (ctx.getDeterminedOutcome().startsWith("Kazuchika Okada defeats")) {
        homeWins++;
      }
    }

    // Without bonus, win rate would be ~50%. With +10% weight, home wrestler should win > 50%
    assertThat(homeWins).isGreaterThan(trials / 2);
  }

  @Test
  void testNoHomeFieldBoostWhenVenueIsNull() {
    WrestlerContext homeCtx = makeContext("Kazuchika Okada", "Japan");
    WrestlerContext awayCtx = makeContext("John Doe", "USA");

    // Null venue — no home boost for either wrestler
    int homeWins = 0;
    int trials = 1000;
    for (int i = 0; i < trials; i++) {
      SegmentNarrationService.SegmentNarrationContext ctx =
          makeContext(List.of(homeCtx, awayCtx), null);
      service.determineOutcomeIfNeeded(ctx);
      if (ctx.getDeterminedOutcome().startsWith("Kazuchika Okada defeats")) {
        homeWins++;
      }
    }

    // Should be approximately 50/50 (within statistical noise over 1000 trials)
    assertThat(homeWins).isBetween(400, 600);
  }

  @Test
  void testNoHomeFieldBoostWhenHeritageMismatch() {
    // Both wrestlers have no heritage match for Tokyo
    WrestlerContext ctx1 = makeContext("Kazuchika Okada", "Brazil");
    WrestlerContext ctx2 = makeContext("John Doe", "USA");
    VenueContext venue = makeVenue("Tokyo, Japan");

    int wrestler1Wins = 0;
    int trials = 1000;
    for (int i = 0; i < trials; i++) {
      SegmentNarrationService.SegmentNarrationContext ctx = makeContext(List.of(ctx1, ctx2), venue);
      service.determineOutcomeIfNeeded(ctx);
      if (ctx.getDeterminedOutcome().startsWith("Kazuchika Okada defeats")) {
        wrestler1Wins++;
      }
    }

    // Neither has a home field advantage — should be ~50/50
    assertThat(wrestler1Wins).isBetween(400, 600);
  }
}

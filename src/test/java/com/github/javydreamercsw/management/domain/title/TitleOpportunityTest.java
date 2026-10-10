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
package com.github.javydreamercsw.management.domain.title;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** State-machine tests for the held briefcase entity (ATW-8p72). */
class TitleOpportunityTest {

  private static final LocalDate EARNED = LocalDate.of(2026, 10, 8);

  private TitleOpportunity held() {
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setEarnedAt(EARNED);
    return opportunity;
  }

  @Test
  @DisplayName("isHeld/isExpired reflect the state and expiry boundary")
  void stateQueries() {
    TitleOpportunity opportunity = held();
    assertTrue(opportunity.isHeld());

    opportunity.setExpiryDate(EARNED.plusDays(365));
    assertFalse(opportunity.isExpired(EARNED.plusDays(365)), "Expiry day itself is still valid");
    assertTrue(opportunity.isExpired(EARNED.plusDays(366)), "Day after expiry is expired");
    assertFalse(opportunity.isExpired(EARNED), "Long before expiry");

    opportunity.setExpiryDate(null);
    assertFalse(opportunity.isExpired(EARNED.plusDays(10_000)), "Null expiry never expires");
  }

  @Test
  @DisplayName("markCashedIn spends a HELD case and records title/segment/date")
  void markCashedIn_spendsHeldCase() {
    TitleOpportunity opportunity = held();
    Title title = new Title();
    Segment segment = new Segment();
    LocalDate gameDate = EARNED.plusDays(30);

    opportunity.markCashedIn(title, segment, gameDate);

    assertEquals(TitleOpportunityStatus.CASHED_IN, opportunity.getStatus());
    assertEquals(title, opportunity.getCashedAgainstTitle());
    assertEquals(segment, opportunity.getCashedAtSegment());
    assertEquals(gameDate, opportunity.getCashedAt());
  }

  @Test
  @DisplayName("markCashedIn rejects a case that is not HELD")
  void markCashedIn_rejectsNonHeld() {
    TitleOpportunity opportunity = held();
    opportunity.setStatus(TitleOpportunityStatus.CASHED_IN);
    assertThrows(
        IllegalStateException.class,
        () -> opportunity.markCashedIn(new Title(), new Segment(), EARNED));

    opportunity.setStatus(TitleOpportunityStatus.EXPIRED);
    assertThrows(
        IllegalStateException.class,
        () -> opportunity.markCashedIn(new Title(), new Segment(), EARNED));
  }

  @Test
  @DisplayName("markExpired flips the status; earnedFromTournamentId is session-safe")
  void markExpiredAndTournamentIdAccessor() {
    TitleOpportunity opportunity = held();
    assertNull(opportunity.getEarnedFromTournamentId(), "No tournament linked yet");

    opportunity.markExpired();
    assertEquals(TitleOpportunityStatus.EXPIRED, opportunity.getStatus());

    Tournament tournament = mock(Tournament.class);
    when(tournament.getId()).thenReturn(42L);
    opportunity.setEarnedFromTournament(tournament);
    assertEquals(42L, opportunity.getEarnedFromTournamentId());
  }
}

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
package com.github.javydreamercsw.management.service.league;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchFulfillmentServiceTest {

  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;

  @InjectMocks private MatchFulfillmentService matchFulfillmentService;

  private MatchFulfillment fulfillment;
  private Wrestler winner;
  private Account user;

  @BeforeEach
  void setUp() {
    fulfillment = new MatchFulfillment();
    winner = new Wrestler();
    winner.setName("Winner");
    user = new Account();

    when(matchFulfillmentRepository.save(any(MatchFulfillment.class)))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  void getFulfillmentWithDetails_found_returnsOptional() {
    when(matchFulfillmentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(fulfillment));

    Optional<MatchFulfillment> result = matchFulfillmentService.getFulfillmentWithDetails(1L);

    assertTrue(result.isPresent());
    assertSame(fulfillment, result.get());
  }

  @Test
  void getFulfillmentWithDetails_notFound_returnsEmpty() {
    when(matchFulfillmentRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

    Optional<MatchFulfillment> result = matchFulfillmentService.getFulfillmentWithDetails(99L);

    assertTrue(result.isEmpty());
  }

  @Test
  void submitResult_notFinalized_setsWinnerAndSubmitter() {
    fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.PENDING_RESULTS);

    MatchFulfillment saved = matchFulfillmentService.submitResult(fulfillment, winner, user);

    assertSame(winner, saved.getReportedWinner());
    assertSame(user, saved.getSubmittedBy());
  }

  @Test
  void submitResult_notFinalized_setsStatusToSubmitted() {
    fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.PENDING_RESULTS);

    MatchFulfillment saved = matchFulfillmentService.submitResult(fulfillment, winner, user);

    assertEquals(MatchFulfillment.FulfillmentStatus.SUBMITTED, saved.getStatus());
  }

  @Test
  void submitResult_notFinalized_savesAndReturns() {
    fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.PENDING_RESULTS);

    MatchFulfillment saved = matchFulfillmentService.submitResult(fulfillment, winner, user);

    verify(matchFulfillmentRepository).save(fulfillment);
    assertSame(fulfillment, saved);
  }

  @Test
  void submitResult_alreadyFinalized_throwsIllegalStateException() {
    fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.FINALIZED);

    assertThrows(
        IllegalStateException.class,
        () -> matchFulfillmentService.submitResult(fulfillment, winner, user));
  }
}

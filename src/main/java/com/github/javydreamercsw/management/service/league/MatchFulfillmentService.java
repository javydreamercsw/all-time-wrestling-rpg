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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchFulfillmentService {

  private final MatchFulfillmentRepository matchFulfillmentRepository;

  @Transactional
  public MatchFulfillment submitResult(
      MatchFulfillment fulfillment, Wrestler winner, Account user) {
    if (fulfillment.getStatus() == MatchFulfillment.FulfillmentStatus.FINALIZED) {
      throw new IllegalStateException("Match result is already finalized.");
    }

    fulfillment.setReportedWinner(winner);
    fulfillment.setSubmittedBy(user);
    fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.SUBMITTED);

    return matchFulfillmentRepository.save(fulfillment);
  }
}

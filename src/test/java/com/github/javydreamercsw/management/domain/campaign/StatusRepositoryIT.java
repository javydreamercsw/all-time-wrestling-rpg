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
package com.github.javydreamercsw.management.domain.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class StatusRepositoryIT extends ManagementIntegrationTest {

  @Autowired private StatusCardRepository statusCardRepository;
  @Autowired private WrestlerStatusRepository wrestlerStatusRepository;
  @Autowired private WrestlerStatusHistoryRepository wrestlerStatusHistoryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  @Transactional
  void testStatusPersistence() {
    // 1. Create and save a StatusCard
    StatusCard card =
        StatusCard.builder()
            .name("Draw / Main Eventer")
            .description("Represents the wrestler's ability to draw a crowd.")
            .positive(true)
            .level1Effect("momentum: +4")
            .level2Effect("momentum: +4, mainEvent: true")
            .build();
    card = statusCardRepository.save(card);
    assertNotNull(card.getId());

    // 2. Create and save a Wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Johnny All Time Persistence");
    wrestler = wrestlerRepository.save(wrestler);
    assertNotNull(wrestler.getId());

    // 3. Create and save a WrestlerStatus
    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();
    status = wrestlerStatusRepository.save(status);
    assertNotNull(status.getId());

    // 4. Create and save a WrestlerStatusHistory
    WrestlerStatusHistory history =
        WrestlerStatusHistory.builder()
            .wrestler(wrestler)
            .statusCard(card)
            .action(WrestlerStatusAction.GAIN)
            .newLevel(1)
            .build();
    history = wrestlerStatusHistoryRepository.save(history);
    assertNotNull(history.getId());

    // 5. Verify queries
    List<WrestlerStatus> activeStatuses = wrestlerStatusRepository.findByWrestler(wrestler);
    assertEquals(1, activeStatuses.size());
    assertEquals(card.getId(), activeStatuses.get(0).getStatusCard().getId());

    List<WrestlerStatusHistory> historyLogs =
        wrestlerStatusHistoryRepository.findByWrestler(wrestler);
    assertEquals(1, historyLogs.size());
    assertEquals(WrestlerStatusAction.GAIN, historyLogs.get(0).getAction());
  }
}

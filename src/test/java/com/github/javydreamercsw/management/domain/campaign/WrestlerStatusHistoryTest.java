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

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WrestlerStatusHistoryTest {

  @Test
  void testWrestlerStatusHistoryEntity() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    StatusCard card = StatusCard.builder().id(10L).key("status_draw").level1Name("Draw").build();

    LocalDateTime now = LocalDateTime.now();
    WrestlerStatusHistory history =
        WrestlerStatusHistory.builder()
            .wrestler(wrestler)
            .statusCard(card)
            .action(WrestlerStatusAction.GAIN)
            .oldLevel(null)
            .newLevel(1)
            .creationDate(now)
            .build();

    assertNotNull(history);
    assertEquals(wrestler, history.getWrestler());
    assertEquals(card, history.getStatusCard());
    assertEquals(WrestlerStatusAction.GAIN, history.getAction());
    assertEquals(null, history.getOldLevel());
    assertEquals(1, history.getNewLevel());
    assertEquals(now, history.getCreationDate());
  }
}

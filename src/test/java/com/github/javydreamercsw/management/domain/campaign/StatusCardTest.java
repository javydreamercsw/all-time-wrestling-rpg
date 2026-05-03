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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusCardTest {

  @Test
  void testStatusCardEntity() {
    StatusCard card =
        StatusCard.builder()
            .name("Draw / Main Eventer")
            .description("Represents the wrestler's ability to draw a crowd.")
            .positive(true)
            .level1Effect("momentum: +4")
            .level2Effect("momentum: +4, mainEvent: true")
            .flipUpCondition("momentum >= 5 or prompt: draw")
            .flipDownCondition("momentum < 3")
            .discardCondition("prompt: remove")
            .build();

    assertNotNull(card);
    assertEquals("Draw / Main Eventer", card.getName());
    assertEquals("Represents the wrestler's ability to draw a crowd.", card.getDescription());
    assertTrue(card.isPositive());
    assertEquals("momentum: +4", card.getLevel1Effect());
    assertEquals("momentum: +4, mainEvent: true", card.getLevel2Effect());
    assertEquals("momentum >= 5 or prompt: draw", card.getFlipUpCondition());
    assertEquals("momentum < 3", card.getFlipDownCondition());
    assertEquals("prompt: remove", card.getDiscardCondition());
  }
}

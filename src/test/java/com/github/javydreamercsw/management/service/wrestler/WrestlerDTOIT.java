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
package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class WrestlerDTOIT extends ManagementIntegrationTest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setUp() {
    dataInitializer.init();
  }

  @Test
  @Transactional
  void robVanDamMoveSetShouldBePopulatedCorrectly() {
    // Given
    String wrestlerName = "Rob Van Dam";

    // When
    Wrestler wrestler =
        wrestlerService
            .findByName(wrestlerName)
            .orElseThrow(() -> new AssertionError("Wrestler " + wrestlerName + " not found"));

    WrestlerDTO wrestlerDTO = new WrestlerDTO(wrestler);

    // Then
    assertThat(wrestlerDTO.getMoveSet()).isNotNull();
    assertThat(wrestlerDTO.getMoveSet().getFinishers())
        .extracting("name")
        .containsExactlyInAnyOrder("Five-Star Frogsplash", "Van Terminator");
    assertThat(wrestlerDTO.getMoveSet().getTrademarks())
        .extracting("name")
        .containsExactlyInAnyOrder(
            "Rolling Thunder",
            "Split-Legged Moonsault",
            "Springboard Thrust Kick",
            "Van Daminator",
            "Corkscrew Legdrop");

    assertThat(wrestlerDTO.getMoveSet().getCommonMoves())
        .extracting("name")
        .containsExactlyInAnyOrder(
            "Monkey Flip",
            "Forearm",
            "Thrust Kick",
            "Handspring Moonsault",
            "Somersault",
            "Flying Cross-Body",
            "Slam",
            "Clothesline",
            "Northern Lights Suplex",
            "Tornado DDT");
  }
}

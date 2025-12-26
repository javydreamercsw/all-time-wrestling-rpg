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
package com.github.javydreamercsw.management.ui.view.booker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BookerViewE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private ShowService showService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private RivalryService rivalryService;

  @Test
  public void testBookerViewLoads() {
    // Create a wrestler
    Wrestler wrestler =
        Wrestler.builder()
            .name("Test Wrestler")
            .isPlayer(true)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .build();
    wrestlerService.save(wrestler);

    Wrestler opponent =
        Wrestler.builder()
            .name("Opponent")
            .isPlayer(false)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .build();
    wrestlerService.save(opponent);

    Assertions.assertNotNull(wrestler.getId());
    Assertions.assertNotNull(opponent.getId());
    rivalryService.createRivalry(wrestler.getId(), opponent.getId(), "Test Rivalry");

    // Create a show
    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Show Description");
    show.setShowDate(LocalDate.now().plusDays(1));
    show.setType(showTypeService.findAll().get(0));
    showService.save(show);

    // Navigate to the BookerView
    assertDoesNotThrow(
        () -> {
          driver.get("http://localhost:" + serverPort + getContextPath() + "/booker");
          // Check that the grids have the correct number of rows
          assertEquals(15, getGridRows("roster-overview-grid").size());
          assertEquals(1, getGridRows("upcoming-shows-grid").size());
          assertEquals(1, getGridRows("active-rivalries-grid").size());

          // Check the content of the grids
          assertGridContains("upcoming-shows-grid", "Test Show");
          assertGridContains(
              "active-rivalries-grid", "Test Wrestler vs Opponent (0 heat - Simmering)");
        });
  }
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

public class BookerViewE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private ShowService showService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private RivalryService rivalryService;

  @BeforeEach
  public void setUp() {
    cleanupLeagues();
  }

  @Test
  public void testBookerViewLoads() {
    // Create several wrestlers so the roster grid has multiple rows
    Wrestler wrestler1 = null;
    Wrestler wrestler3 = null;
    for (int i = 1; i <= 3; i++) {
      Wrestler wrestler =
          Wrestler.builder().name("Test Wrestler " + i).isPlayer(true).gender(Gender.MALE).build();
      wrestler = wrestlerService.save(wrestler);
      if (i == 1) {
        wrestler1 = wrestler;
      }
      if (i == 3) {
        wrestler3 = wrestler;
      }

      WrestlerState state =
          wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
      state.setTier(WrestlerTier.MIDCARDER);
      wrestlerStateRepository.save(state);
    }

    // Two rivalries (disjoint pairs) so the rivalries grid has multiple rows
    Wrestler opponentA =
        Wrestler.builder().name("Opponent A").isPlayer(false).gender(Gender.MALE).build();
    opponentA = wrestlerService.save(opponentA);
    Wrestler opponentB =
        Wrestler.builder().name("Opponent B").isPlayer(false).gender(Gender.MALE).build();
    opponentB = wrestlerService.save(opponentB);

    for (Wrestler opponent : List.of(opponentA, opponentB)) {
      WrestlerState opponentState =
          wrestlerService.getOrCreateState(opponent.getId(), defaultUniverse.getId());
      opponentState.setTier(WrestlerTier.MIDCARDER);
      wrestlerStateRepository.save(opponentState);
    }

    Assertions.assertNotNull(wrestler3.getId());
    rivalryService.createRivalry(wrestler1.getId(), opponentA.getId(), "Test Rivalry");
    rivalryService.createRivalry(wrestler3.getId(), opponentB.getId(), "Second Rivalry");

    // Ensure Weekly show type exists
    showTypeService.createOrUpdateShowType("Weekly", "Weekly Show", 4, 2);

    // Create two upcoming shows so the shows grid has multiple rows
    for (String showName : List.of("Test Show", "Second Test Show")) {
      Show show = new Show();
      show.setName(showName);
      show.setDescription(showName + " Description");
      show.setShowDate(LocalDate.now().plusDays(1));
      show.setType(showTypeService.findByName("Weekly").get());
      show.setUniverse(defaultUniverse);
      showService.save(show);
    }

    // Navigate to the BookerView
    assertDoesNotThrow(
        () -> {
          navigateTo("booker");
          // Check that the grids have the correct number of rows
          assertFalse(getGridRows("roster-overview-grid").isEmpty());
          assertMultipleRowsVisible("roster-overview-grid");

          // Click on the "Upcoming Shows" tab
          click("vaadin-tab", "Upcoming Shows");
          waitForVaadinElementVisible(By.id("upcoming-shows-grid"));
          assertFalse(getGridRows("upcoming-shows-grid").isEmpty());
          assertMultipleRowsVisible("upcoming-shows-grid");
          assertGridContains("upcoming-shows-grid", "Test Show");

          // Click on the "Active Rivalries" tab
          click("vaadin-tab", "Active Rivalries");
          waitForVaadinElementVisible(By.id("active-rivalries-grid"));
          assertFalse(getGridRows("active-rivalries-grid").isEmpty());
          assertMultipleRowsVisible("active-rivalries-grid");
          assertGridContains(
              "active-rivalries-grid", "Test Wrestler 3 vs Opponent B (0 heat - Simmering)");
        });
  }

  /**
   * Regression guard for ATW-skkx: the grid must render MORE than one row inside the visible
   * viewport. The bug collapsed each tab's grid to a single visible row (the vaadin-grid rendered
   * at its theme-minimum height), which row-content assertions cannot detect. Waits until at least
   * two body rows intersect the grid's bounding box (polls to ride out layout settling).
   */
  private void assertMultipleRowsVisible(String gridId) {
    WebElement grid = driver.findElement(By.id(gridId));
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            d -> {
              Long count = countRowsInViewport(grid);
              return count != null && count >= 2;
            });
  }

  private Long countRowsInViewport(WebElement grid) {
    return (Long)
        ((JavascriptExecutor) driver)
            .executeScript(
                """
                const grid = arguments[0];
                const table = grid.shadowRoot && grid.shadowRoot.querySelector('table#table');
                if (!table) return 0;
                const viewport = grid.getBoundingClientRect();
                let visible = 0;
                for (const tr of table.querySelectorAll('tbody tr')) {
                  const rect = tr.getBoundingClientRect();
                  if (rect.bottom > viewport.top && rect.top < viewport.bottom
                      && rect.height > 0) {
                    visible++;
                  }
                }
                return visible;
                """,
                grid);
  }
}

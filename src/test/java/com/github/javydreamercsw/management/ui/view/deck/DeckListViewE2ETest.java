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
package com.github.javydreamercsw.management.ui.view.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DeckListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testNavigateToDeckListView() {
    // Retry-enabled navigation — a login redirect race can swallow a plain driver.get() and
    // leave the browser on the Home view instead of the deck list.
    navigateToAndWaitForElement("deck-list", By.tagName("vaadin-grid"));

    WebElement grid = waitForVaadinElementVisible(By.tagName("vaadin-grid"));
    assertNotNull(grid);
  }

  @Test
  public void testGridSize() {
    navigateToAndWaitForElement("deck-list", By.tagName("vaadin-grid"));

    WebElement grid = waitForVaadinElementVisible(By.tagName("vaadin-grid"));
    assertNotNull(grid);

    // Check that the grid and its container are full size
    assertThat(grid.getSize().getHeight()).isGreaterThan(0);

    WebElement parent = grid.findElement(By.xpath(".."));
    assertThat(parent.getSize().getHeight()).isGreaterThan(0);
  }

  /**
   * Regression test for LazyInitializationException in DeckListView.openDeckView().
   *
   * <p>Before the fix, clicking "View" passed a raw lazy Set to cardGrid.setItems(), which caused
   * Vaadin's DataCommunicator to call ListDataProvider.size() during WebSocket push — outside any
   * open Hibernate session — throwing a LazyInitializationException.
   */
  @Test
  public void testViewDeckDialogOpensWithCardGrid() {
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(
            Wrestler.builder()
                .name("Deck View Dialog Regression Test")
                .startingHealth(100)
                .startingStamina(100)
                .active(true)
                .build());

    Deck deck = new Deck();
    deck.setWrestler(wrestler);
    deck.setCreationDate(Instant.now());
    deckRepository.saveAndFlush(deck);

    navigateToAndWaitForElement("deck-list", By.tagName("vaadin-grid"));
    waitForVaadinElementVisible(By.tagName("vaadin-grid"));

    clickElement(By.xpath("//vaadin-button[text()='View']"));

    WebDriverWait wait = new WebDriverWait(driver, getWaitTimeout());
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-dialog")));
    assertNotNull(driver.findElement(By.tagName("vaadin-grid")));
  }
}

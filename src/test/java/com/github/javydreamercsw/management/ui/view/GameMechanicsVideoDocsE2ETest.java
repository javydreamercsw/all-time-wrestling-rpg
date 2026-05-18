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
package com.github.javydreamercsw.management.ui.view;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

class GameMechanicsVideoDocsE2ETest extends AbstractVideoDocsE2ETest {

  @Test
  void testRecordCardListWalkthrough() {
    setVideoInfo("Game Mechanics", "Cards Walkthrough", "mechanic-cards-walkthrough");

    navigateTo("card-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "The heart of the ATW RPG battle system. Each card represents a move — Strike, Grapple,"
            + " Aerial, or Throw — with specific health and stamina costs and damage effects.");

    // Scroll down to show more cards
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400)");
    waitForVaadinClientToLoad();
    sleep(2000);
    captureCaption("Browse your full card collection. Click any card to see its stat breakdown.");

    // Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1500);

    // Click the first row if available
    try {
      WebElement firstRow = driver.findElement(By.cssSelector("vaadin-grid-cell-content"));
      clickElement(firstRow);
      waitForVaadinClientToLoad();
      captureCaption(
          "Card detail view: health cost, stamina cost, damage type, and move category.");
      sleep(2000);
    } catch (Exception ignored) {
    }
  }

  @Test
  void testRecordDeckListWalkthrough() {
    setVideoInfo("Game Mechanics", "Decks Walkthrough", "mechanic-decks-walkthrough");

    navigateTo("deck-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "Manage wrestler-specific decks. Each wrestler can have one active deck used in matches.");

    sleep(2000);
    captureCaption(
        "Strategy: balance high-damage finishers with efficient setup moves"
            + " and stamina-recovering taunts.");

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    sleep(1500);
  }

  @Test
  void testRecordWrestlerRankingsWalkthrough() {
    setVideoInfo("Dashboards", "Wrestler Rankings", "dashboard-rankings-walkthrough");

    navigateTo("wrestler-rankings");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "Track the momentum of every wrestler in the promotion."
            + " Rankings determine title eligibility and positioning on show cards.");

    sleep(2000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400)");
    sleep(1500);
    captureCaption(
        "Wrestlers accumulate ranking points through wins, title matches, and fan reactions.");

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}

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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

@Tag("video")
class GameMechanicsVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void testRecordCardListWalkthrough() {
    setVideoInfo("Game Mechanics", "Cards Walkthrough", "mechanic-cards-walkthrough");

    navigateTo("card-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "The heart of the ATW RPG battle system. Each card represents a move — Strike, Grapple,"
            + " Aerial, or Throw — with specific health and stamina costs and damage effects."
            + " Wrestlers draw from their personal deck each round of a match.",
        4500);

    // Scroll down to show more cards
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400)");
    waitForVaadinClientToLoad();
    sleep(2000);
    captureCaption(
        "Browse your full card collection. Click any card to see its stat breakdown —"
            + " health cost, stamina cost, damage type, and which move category it belongs to.",
        3500);

    // Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1500);

    // Click the first row if available
    WebElement firstRow = driver.findElement(By.cssSelector("vaadin-grid-cell-content"));
    clickElement(firstRow);
    waitForVaadinClientToLoad();
    captureCaption(
        "Card detail view shows health cost, stamina cost, damage type, and move category."
            + " These values are referenced directly by the match engine and AI narration"
            + " when describing what happened in a segment.",
        4000);
    sleep(2000);
  }

  @Test
  void testRecordDeckListWalkthrough() {
    setVideoInfo("Game Mechanics", "Decks Walkthrough", "mechanic-decks-walkthrough");

    navigateTo("deck-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "Deck List — manage wrestler-specific decks. Each wrestler can have one active deck"
            + " used in matches; the deck determines which moves the AI draws from when"
            + " generating match narration and outcomes.",
        4000);

    sleep(2000);
    captureCaption(
        "These move sets are what the AI draws from during match narration — a richer deck"
            + " with varied move types leads to more dynamic and realistic commentary.",
        3500);

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
        "Wrestler Rankings — track the momentum of every wrestler in the promotion."
            + " Rankings are determined by fan count and determine title eligibility,"
            + " match positioning, and contendership on upcoming show cards.",
        4500);

    sleep(2000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400)");
    sleep(1500);
    captureCaption(
        "Wrestlers accumulate fans through wins, title matches, and fan reactions — losses"
            + " and inactivity erode the fan base over time, creating natural momentum shifts"
            + " that drive realistic storyline booking decisions.",
        4000);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);
  }
}

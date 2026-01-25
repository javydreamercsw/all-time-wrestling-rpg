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

import com.github.javydreamercsw.AbstractE2ETest;
import org.junit.jupiter.api.Test;

class GameMechanicsDocsE2ETest extends AbstractE2ETest {

  @Test
  void testCaptureCardListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    waitForVaadinClientToLoad();
    waitForText("Card List");

    documentFeature(
        "Game Mechanics",
        "Cards",
        "The heart of the ATW RPG battle system. Each card represents a move (Strike, Grapple,"
            + " Aerial, Throw) with specific health and stamina costs and damage effects.",
        "mechanic-cards");
  }

  @Test
  void testCaptureDeckListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/deck-list");
    waitForVaadinClientToLoad();
    waitForText("Deck List");

    documentFeature(
        "Game Mechanics",
        "Decks",
        "Manage wrestler-specific decks. Strategy involves balancing high-damage finishers with"
            + " efficient setup moves and stamina-recovering taunts.",
        "mechanic-decks");
  }

  @Test
  void testCaptureWrestlerRankingsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");
    waitForVaadinClientToLoad();
    waitForText("Rankings");

    documentFeature(
        "Dashboards",
        "Wrestler Rankings",
        "Track the momentum of every wrestler in the promotion. Rankings determine title"
            + " eligibility and positioning on show cards.",
        "dashboard-rankings");
  }

  @Test
  void testCaptureShowCalendarView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-calendar");
    waitForVaadinClientToLoad();
    waitForText("Show Calendar");

    documentFeature(
        "Dashboards",
        "Show Calendar",
        "Plan ahead with the integrated calendar. View upcoming weekly shows and major Premium"
            + " Live Events at a glance.",
        "dashboard-calendar");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

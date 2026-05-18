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

class GameMechanicsVideoDocsE2ETest extends AbstractVideoDocsE2ETest {

  @Test
  void testRecordCardListWalkthrough() {
    recordFeatureVideo(
        "Game Mechanics",
        "Cards Walkthrough",
        "The heart of the ATW RPG battle system. Each card represents a move — Strike, Grapple,"
            + " Aerial, or Throw — with specific health and stamina costs and damage effects."
            + " Browse your full collection and click any card to see its stat breakdown.",
        "mechanic-cards-walkthrough",
        () -> {
          navigateTo("card-list");
          waitForVaadinClientToLoad();
          waitForVaadinElement(driver, By.tagName("vaadin-grid"));
        });
  }

  @Test
  void testRecordDeckListWalkthrough() {
    recordFeatureVideo(
        "Game Mechanics",
        "Decks Walkthrough",
        "Manage wrestler-specific decks. Strategy involves balancing high-damage finishers with"
            + " efficient setup moves and stamina-recovering taunts."
            + " Each wrestler can have one active deck used in matches.",
        "mechanic-decks-walkthrough",
        () -> {
          navigateTo("deck-list");
          waitForVaadinClientToLoad();
          waitForVaadinElement(driver, By.tagName("vaadin-grid"));
        });
  }

  @Test
  void testRecordWrestlerRankingsWalkthrough() {
    recordFeatureVideo(
        "Dashboards",
        "Wrestler Rankings Walkthrough",
        "Track the momentum of every wrestler in the promotion."
            + " Rankings determine title eligibility and positioning on show cards.",
        "dashboard-rankings-walkthrough",
        () -> {
          navigateTo("wrestler-rankings");
          waitForVaadinClientToLoad();
          waitForVaadinElement(driver, By.tagName("vaadin-grid"));
        });
  }
}

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

class EntityDocsE2ETest extends AbstractE2ETest {

  @Test
  void testCaptureFactionListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    waitForText("Faction List");

    documentFeature(
        "Entities",
        "Factions",
        "Manage powerful wrestling groups. Factions bring together multiple wrestlers under a"
            + " single banner, creating complex storylines and alliance-based rivalries.",
        "entity-factions");
  }

  @Test
  void testCaptureTeamsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/teams");
    waitForVaadinClientToLoad();
    waitForText("Teams");

    documentFeature(
        "Entities",
        "Tag Teams",
        "Track formal tag team partnerships. Teams often have their own specific finishing moves"
            + " and shared history in the Tag Team division.",
        "entity-teams");
  }

  @Test
  void testCaptureTitleListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/title-list");
    waitForVaadinClientToLoad();
    waitForText("Titles");

    documentFeature(
        "Entities",
        "Championships",
        "Manage the various titles across the promotion. From World Championships to regional"
            + " belts, track current champions and the prestige of each title.",
        "entity-titles");
  }

  @Test
  void testCaptureSeasonListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/season-list");
    waitForVaadinClientToLoad();
    waitForText("Seasons");

    documentFeature(
        "Entities",
        "Seasons",
        "Configure and track game seasons. Seasons group shows together and define the journey"
            + " toward major year-end events.",
        "entity-seasons");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

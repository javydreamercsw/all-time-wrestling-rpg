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
package com.github.javydreamercsw.management.ui.view.admin;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class WrestlerRelationshipDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureAdminRelationshipsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    WebElement tab =
        waitForVaadinElement(
            driver,
            org.openqa.selenium.By.xpath(
                "//vaadin-tab[contains(text(), 'Wrestler Relationships')]"));
    clickElement(tab);

    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {
    }

    documentFeature(
        "Admin",
        "Wrestler Relationships Management",
        "Manage the social fabric of your promotion. Define marriages, family ties, and deep"
            + " friendships that influence chemistry, AI narration, and random backstage events.",
        "admin-wrestler-relationships");
  }

  @Test
  void testCaptureWrestlerProfileRelationships() {
    Wrestler johnny =
        wrestlerRepository
            .findByName("Johnny All Time")
            .orElseThrow(() -> new RuntimeException("Johnny All Time not found"));

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();

    // Use the action menu to navigate to profile
    WebElement actionMenu =
        waitForVaadinElement(driver, org.openqa.selenium.By.id("action-menu-" + johnny.getId()));

    selectFromVaadinMenuBar(actionMenu, "View Profile");
    waitForVaadinClientToLoad();
    waitForText("Relationships");

    documentFeature(
        "Wrestler Profile",
        "Personal Relationships",
        "Wrestler profiles now include a 'Relationships' section showing their social connections."
            + " These bonds provide chemistry bonuses when related wrestlers perform together.",
        "wrestler-profile-relationships");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

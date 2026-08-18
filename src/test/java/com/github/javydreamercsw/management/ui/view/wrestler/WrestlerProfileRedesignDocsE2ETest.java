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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class WrestlerProfileRedesignDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureRedesignedProfile() {
    Wrestler wrestler =
        wrestlerRepository
            .findByName("Johnny All Time")
            .orElseThrow(() -> new RuntimeException("Johnny All Time not found"));

    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler.getId());
    waitForVaadinClientToLoad();

    waitForText("Biography");
    waitForText("Career Stats");

    documentFeature(
        "Wrestler Profile",
        "Modernized Wrestler Dashboard",
        """
        The redesigned wrestler profile features a prominent 'Hero' section with the wrestler's\
         image, biography, and personal relationships always visible. Secondary information\
         is organized into a clean, mobile-friendly accordion layout.\
        """,
        "wrestler-profile-redesign");

    expandAccordionPanel("Abilities");
    waitForText("Core Abilities");

    documentFeature(
        "Wrestler Profile",
        "Wrestler Abilities Panel",
        """
        The Abilities panel groups each wrestler's moves into Core Abilities (shared by all\
         wrestlers) and Wrestler Abilities (superstar-specific). Limited-use abilities show a\
         colored chip with their use count. Unlockable abilities are collapsed by default.\
        """,
        "wrestler-profile-abilities");
  }

  private void waitForText(final String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(., '" + text + "')]"));
  }

  private void expandAccordionPanel(final String label) {
    waitForVaadinClientToLoad();
    List<WebElement> panels = driver.findElements(By.tagName("vaadin-accordion-panel"));
    for (WebElement panel : panels) {
      if (panel.getText().contains(label)) {
        clickElement(panel);
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
        return;
      }
    }
    throw new RuntimeException("Could not find accordion panel with label: " + label);
  }
}

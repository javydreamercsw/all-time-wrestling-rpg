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

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class AdminDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private InjuryTypeRepository injuryTypeRepository;

  @Test
  void testCaptureAdminToolsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    // Default tab is Admin Tools
    waitForText("Recalculate Wrestler Tiers");

    documentFeature(
        "Admin",
        "Admin Tools",
        "Perform critical maintenance tasks such as manual tier recalculation and account"
            + " management.",
        "admin-tools");
  }

  @Test
  void testCaptureAiSettingsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    waitForVaadinClientToLoad();

    // Click the AI Settings tab more robustly
    WebElement tab =
        waitForVaadinElement(
            driver, org.openqa.selenium.By.xpath("//vaadin-tab[contains(text(), 'AI Settings')]"));

    clickElement(tab);

    // Give it a moment to transition visibility
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {
    }
    takeSequencedScreenshot("after-ai-tab-click");
    waitForText("Common AI Settings");

    documentFeature(
        "Admin",
        "AI Configuration",
        "Configure the Artificial Intelligence providers used for match narration, image"
            + " generation, and creative assistance. You can switch between different LLM providers"
            + " (OpenAI, Anthropic, Gemini, LocalAI) and configure their specific settings.",
        "admin-ai-settings");
  }

  @Test
  void testCaptureInjuryTypesView() {
    // Populate with sample data
    if (injuryTypeRepository.count() == 0) {
      InjuryType concussion = new InjuryType();
      concussion.setInjuryName("Concussion");
      concussion.setHealthEffect(-5);
      concussion.setStaminaEffect(-5);
      concussion.setCardEffect(-1);
      concussion.setSpecialEffects("Cannot perform high-risk moves.");

      injuryTypeRepository.save(concussion);

      InjuryType kneeInjury = new InjuryType();
      kneeInjury.setInjuryName("Torn ACL");
      kneeInjury.setHealthEffect(-3);
      kneeInjury.setStaminaEffect(-8);
      kneeInjury.setCardEffect(-2);
      kneeInjury.setSpecialEffects("Movement speed reduced.");
      injuryTypeRepository.save(kneeInjury);
    }

    driver.get("http://localhost:" + serverPort + getContextPath() + "/injury-types");

    waitForVaadinClientToLoad();

    waitForText("Injury Types");

    documentFeature(
        "Admin",
        "Injury Management",
        "Define different types of injuries, their severity, and recovery times. These are used"
            + " to dynamically affect wrestler performance and availability in the campaign and"
            + " booking modes.",
        "admin-injury-types");
  }

  @Test
  void testCaptureWrestlerListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    waitForText("Wrestlers");

    documentFeature(
        "Admin",
        "Wrestler Management",
        "Manage the entire roster of wrestlers. Add new talent, edit existing stats, assign images,"
            + " and manage contract details from this centralized view.",
        "admin-wrestler-list");
  }

  @Test
  void testCaptureBackstageActionsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-template-list");
    waitForVaadinClientToLoad();
    waitForText("Show Templates");

    documentFeature(
        "Admin",
        "Show Templates",
        "Define reusable templates for your shows. Set up standard segments, match orders, and"
            + " branding to quickly book consistent weekly episodes or pay-per-views.",
        "admin-show-templates");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

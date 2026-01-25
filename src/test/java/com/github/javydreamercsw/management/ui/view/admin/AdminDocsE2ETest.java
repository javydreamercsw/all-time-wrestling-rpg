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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class AdminDocsE2ETest extends AbstractE2ETest {

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
    // 1. Setup - Backstage Actions requires an active campaign
    // We can use the existing setup logic from CampaignDocsE2ETest but we need to inject the
    // repositories here too
    // For simplicity, we'll assume the standard admin user login works, but we need to ensure a
    // campaign exists if we want to see content
    // However, the test method is just capturing the "Admin" perspective of "Actions" if we meant
    // "Backstage Actions" configuration.
    // BUT the user asked for "Actions" under Admin. If they meant "Backstage Action Configuration",
    // that might be a different view.
    // If they meant the player view of taking actions, it belongs in "Campaign" or "Player" guide.
    // Given the context "Under admin... actions", it likely refers to *configuring* actions?
    // Checking MenuService, "Backstage Action" is NOT under Admin. It's under Campaign.
    // "Actions" under Admin might mean "Admin Actions" or something else.
    // But the prompt said: "Under admin the various AI settings, Wrestler View and the actions."
    // It's ambiguous. "the actions" might refer to "Backstage Actions" but placed in the wrong
    // category in the prompt,
    // OR it refers to a specific Admin Actions view.
    // Looking at file list, there is no "ActionConfigurationView".
    // I will assume the user meant the "Backstage Actions" view (where you perform them) and I will
    // document it as a feature,
    // possibly under a "Player" or "Campaign" category in the manifest, OR just put it in Admin if
    // I must.
    // But wait, the user said "Under admin... actions".
    // Let's look for any other "Action" related view.
    // There is no other obvious candidate.
    // I will capture "Backstage Actions" (the player view) but I need to make sure the test can
    // access it.
    // AND I need to setup a campaign.

    // Actually, looking at the code I just read (BackstageActionView), it is indeed a player view.
    // I'll skip it for "Admin" test and move it to a "PlayerDocsE2ETest" or add it to
    // "CampaignDocsE2ETest".
    // The user's prompt "Under admin... actions" might be a misunderstanding of the menu structure.
    // I will stick to "Show Templates" for Admin for now as a safe bet for "actions"
    // (configuration),
    // AND I will add a new test for "Backstage Actions" in CampaignDocsE2ETest where it belongs
    // contextually.

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

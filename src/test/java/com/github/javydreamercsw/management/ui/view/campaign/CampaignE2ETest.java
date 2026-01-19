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
package com.github.javydreamercsw.management.ui.view.campaign;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

class CampaignE2ETest extends AbstractE2ETest {

  @Test
  void testCampaignFlow() {
    // 1. Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("campaign-dashboard-initial");

    // 2. Start Campaign if not started (Debug button)
    // We expect "Start New Campaign (Debug)" if no campaign exists.
    // Or "Campaign: All or Nothing" if it does.

    boolean campaignExists = driver.getPageSource().contains("Campaign: All or Nothing");

    if (!campaignExists) {
      WebElement startButton =
          waitForVaadinElement(
              driver, By.xpath("//vaadin-button[text()='Start New Campaign (Debug)']"));
      clickElement(startButton);
      waitForVaadinClientToLoad();
      takeSequencedScreenshot("campaign-started");

      // Verify dashboard loaded
      assertTrue(driver.getPageSource().contains("Campaign: All or Nothing"));
      assertTrue(driver.getPageSource().contains("Chapter"));
      assertTrue(driver.getPageSource().contains("Victory Points"));
    }

    // 3. Navigate to Backstage Actions
    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Backstage Actions']"));
    clickElement(actionsButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("backstage-actions-view");

    // Verify we are on Backstage Actions view
    assertTrue(driver.getPageSource().contains("Backstage Actions"));
    assertTrue(driver.getPageSource().contains("Training (Drive)"));

    // 4. Perform an action (Training)
    WebElement trainingButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Training (Drive)']"));
    clickElement(trainingButton);

    // Verify notification (Success or Fail) - difficult to catch notification in E2E sometimes, but
    // we can check if it didn't crash.
    takeSequencedScreenshot("after-training-action");

    // 5. Navigate back
    WebElement backButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Back to Dashboard']"));
    clickElement(backButton);
    waitForVaadinClientToLoad();

    assertTrue(driver.getPageSource().contains("Campaign: All or Nothing"));
  }
}

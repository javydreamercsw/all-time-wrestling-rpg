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
    // 1. Assign a wrestler to the admin account first
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("wrestler-list");

    // Click the first action menu
    WebElement firstMenu =
        waitForVaadinElement(driver, By.xpath("//vaadin-menu-bar[contains(@id, 'action-menu-')]"));
    selectFromVaadinMenuBar(firstMenu, "Edit");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("edit-wrestler-dialog");

    // Check Is Player
    WebElement isPlayerCheckbox =
        waitForVaadinElement(driver, By.id("wrestler-dialog-is-player-field"));
    if (!isPlayerCheckbox.isSelected()) {
      clickElement(isPlayerCheckbox);
    }

    // Select admin account
    WebElement accountCombo =
        waitForVaadinElement(driver, By.id("wrestler-dialog-account-combo-box"));
    selectFromVaadinComboBox(accountCombo, "admin");

    // Save
    WebElement saveButton = waitForVaadinElement(driver, By.id("wrestler-dialog-save-button"));
    clickElement(saveButton);
    waitForVaadinClientToLoad();

    // 2. Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("campaign-dashboard-initial");

    // 3. Start Campaign if not started (Debug button)
    boolean campaignExists = driver.getPageSource().contains("Campaign: All or Nothing");

    if (!campaignExists) {
      WebElement startButton = waitForVaadinElement(driver, By.id("debug-start-campaign"));
      clickElement(startButton);
      waitForVaadinClientToLoad();
      takeSequencedScreenshot("campaign-started");

      // Verify dashboard loaded
      waitForText("Campaign: All or Nothing");
      assertTrue(driver.getPageSource().contains("Chapter"));
      assertTrue(driver.getPageSource().contains("Victory Points"));
    }

    // 4. Navigate to Backstage Actions
    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Backstage Actions']"));
    clickElement(actionsButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("backstage-actions-view");

    // Verify we are on Backstage Actions view
    waitForText("Backstage Actions");
    assertTrue(driver.getPageSource().contains("Training (Drive)"));

    // 5. Perform an action (Training)
    WebElement trainingButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Training (Drive)']"));
    clickElement(trainingButton);

    // Verify notification (Success or Fail)
    takeSequencedScreenshot("after-training-action");

    // 6. Navigate back
    WebElement backButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Back to Dashboard']"));
    clickElement(backButton);
    waitForVaadinClientToLoad();

    waitForText("Campaign: All or Nothing");
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

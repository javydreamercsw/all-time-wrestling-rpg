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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.AbstractE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

class ExpansionNPCManagementE2ETest extends AbstractE2ETest {

  @Test
  void testToggleNPCExpansionAndVerifyFiltering() {
    // 1. Navigate to Admin and select Expansion Management tab
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    WebElement tab =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]"));
    clickElement(tab);

    // Wait for grid to load expansions
    waitForVaadinElement(
        driver, By.xpath("//vaadin-grid-cell-content[contains(., 'Hurt Business Expansion')]"));

    // 2. Find the checkbox for 'HURT_BUSINESS' and disable it
    WebElement checkbox = waitForVaadinElement(driver, By.id("expansion-toggle-HURT_BUSINESS"));

    // Use Javascript to click to be more robust
    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("arguments[0].click();", checkbox);

    // Wait for notification
    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'disabled')]"));

    // Give it a moment for cache eviction
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }

    // 3. Navigate to Wrestler List and verify MVP (HURT_BUSINESS set) is hidden from Manager column
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();

    // Verify 'MVP' is NOT present in the Manager column
    // (Note: Bobby Lashley is Base Game, but managed by MVP)
    assertThat(driver.findElements(By.xpath("//vaadin-grid-cell-content[contains(., 'MVP')]")))
        .isEmpty();

    // 4. Go back and re-enable it
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();
    clickElement(
        waitForVaadinElement(
            driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]")));

    checkbox = waitForVaadinElement(driver, By.id("expansion-toggle-HURT_BUSINESS"));

    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("arguments[0].click();", checkbox);

    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'enabled')]"));

    // Give it a moment
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }

    // 5. Verify MVP is back in the Wrestler List grid
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//vaadin-grid-cell-content[contains(., 'MVP')]"));
  }
}

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

class ExpansionManagementE2ETest extends AbstractE2ETest {

  @Test
  void testToggleExpansionAndVerifyFiltering() {
    // 1. Navigate to Admin and select Expansion Management tab
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    WebElement tab =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]"));
    clickElement(tab);

    // Wait for grid to load expansions
    waitForVaadinElement(
        driver, By.xpath("//vaadin-grid-cell-content[contains(., 'ATW Extreme Expansion')]"));

    // 2. Find the checkbox for 'Extreme Pack' and disable it
    WebElement extremeCheckbox = waitForVaadinElement(driver, By.id("expansion-toggle-EXTREME"));

    // Use Javascript to click to be more robust with Vaadin Grid rendering
    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("arguments[0].click();", extremeCheckbox);

    // Wait for notification
    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'disabled')]"));

    // Give it a moment for cache eviction to process
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }

    // 3. Navigate to Wrestler List and verify Rob Van Dam (EXTREME set) is hidden
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();

    // Verify 'Rob Van Dam' is NOT present
    assertThat(driver.findElements(By.xpath("//*[contains(text(), 'Rob Van Dam')]"))).isEmpty();

    // 4. Go back and re-enable it
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();
    clickElement(
        waitForVaadinElement(
            driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]")));

    extremeCheckbox = waitForVaadinElement(driver, By.id("expansion-toggle-EXTREME"));

    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("arguments[0].click();", extremeCheckbox);

    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'enabled')]"));

    // Give it a moment
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }

    // 5. Verify Rob Van Dam is back
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Rob Van Dam')]"));
  }
}

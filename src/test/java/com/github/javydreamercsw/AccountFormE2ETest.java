/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.security.test.context.support.WithMockUser;

public class AccountFormE2ETest extends AbstractE2ETest {

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testPasswordVisibilityToggle() {
    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the edit button for the admin account (ID 1)
    WebElement editButton = driver.findElement(By.id("edit-button-1"));
    editButton.click();

    // Wait for the form to load by waiting for the username field
    waitForVaadinElement(driver, By.id("username-field"));
  }

  private Object executeScript(String script, WebElement element) {
    return ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script, element);
  }
}

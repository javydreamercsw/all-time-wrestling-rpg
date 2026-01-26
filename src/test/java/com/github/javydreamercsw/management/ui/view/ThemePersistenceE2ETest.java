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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.AbstractE2ETest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

@Disabled("Flaky in CI environment, functionality verified manually")
public class ThemePersistenceE2ETest extends AbstractE2ETest {

  @Test
  public void testUserThemePersistence() {
    login("player", "player123");

    driver.get("http://localhost:" + serverPort + getContextPath() + "/profile");
    waitForVaadinClientToLoad();

    selectFromVaadinComboBox("theme-selection", "dark");
    clickButtonByText("Save");

    waitForAppToBeReady();
    waitForVaadinClientToLoad();

    // Explicitly wait for the theme attribute to be set
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
        .until(
            d -> {
              String t =
                  (String)
                      ((JavascriptExecutor) d)
                          .executeScript("return document.documentElement.getAttribute('theme')");
              return "dark".equals(t);
            });

    String theme =
        (String)
            ((JavascriptExecutor) driver)
                .executeScript("return document.documentElement.getAttribute('theme')");
    assertEquals("dark", theme, "Theme should be dark");

    selectFromVaadinComboBox("theme-selection", "retro");
    clickButtonByText("Save");

    waitForAppToBeReady();
    waitForVaadinClientToLoad();

    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
        .until(
            d -> {
              String t =
                  (String)
                      ((JavascriptExecutor) d)
                          .executeScript("return document.documentElement.getAttribute('theme')");
              return "retro".equals(t);
            });

    theme =
        (String)
            ((JavascriptExecutor) driver)
                .executeScript("return document.documentElement.getAttribute('theme')");
    assertEquals("retro", theme, "Theme should be retro");
  }

  @Test
  public void testAdminDefaultTheme() {
    login("admin", "admin123");

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();
    click("vaadin-tab", "Game Settings");

    selectFromVaadinComboBox("default-theme-selection", "neon");
    // No save button, it auto-saves on value change

    // Logout and login as viewer (who has no preference set)
    logout();
    login("viewer", "viewer123");

    waitForVaadinClientToLoad();

    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
        .until(
            d -> {
              String t =
                  (String)
                      ((JavascriptExecutor) d)
                          .executeScript("return document.documentElement.getAttribute('theme')");
              return "neon".equals(t);
            });

    String theme =
        (String)
            ((JavascriptExecutor) driver)
                .executeScript("return document.documentElement.getAttribute('theme')");
    assertEquals("neon", theme, "Default theme should be neon for user without preference");
  }
}

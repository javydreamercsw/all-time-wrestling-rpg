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
package com.github.javydreamercsw.management.ui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class MainLayoutE2ETest extends AbstractE2ETest {

  @Test
  void testGithubLink() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinClientToLoad();

    // Wait for the footer to be present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-app-layout")));

    // Find the link
    WebElement link = driver.findElement(By.linkText("Source Code"));

    // Assert that the link is visible and enabled
    assertTrue(link.isDisplayed());
    assertTrue(link.isEnabled());

    // Assert that the href is correct
    assertEquals(
        "https://github.com/javydreamercsw/all-time-wrestling-rpg", link.getAttribute("href"));
  }
}

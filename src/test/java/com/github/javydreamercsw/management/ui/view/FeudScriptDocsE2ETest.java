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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

class FeudScriptDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void captureRivalryListView() {
    navigateTo("rivalry-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    documentFeature(
        "Booker",
        "Rivalry List",
        "Browse all active rivalries between wrestlers. Click any row to open the rivalry detail"
            + " view where you can manage story arc scripts.",
        "booker-rivalry-list");
  }

  @Test
  void captureRivalryDetailView() {
    navigateTo("rivalry-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Click the first row to open the detail view
    driver.findElements(By.cssSelector("vaadin-grid-cell-content")).stream()
        .filter(e -> !e.getText().isBlank())
        .findFirst()
        .ifPresent(WebElement::click);

    waitForVaadinElement(
        driver, By.xpath("//*[contains(@class,'rivalry-detail') or contains(.,'vs')]"));
    documentFeature(
        "Booker",
        "Rivalry Detail",
        "View rivalry details including heat, storyline notes, and dates. Use the Story Arc button"
            + " to script a multi-beat feud arc for this rivalry.",
        "booker-rivalry-detail");
  }

  @Test
  void captureStoryArcWizard() {
    navigateTo("rivalry-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    driver.findElements(By.cssSelector("vaadin-grid-cell-content")).stream()
        .filter(e -> !e.getText().isBlank())
        .findFirst()
        .ifPresent(WebElement::click);

    waitForVaadinElement(driver, By.xpath("//*[text()='Story Arc']"));
    driver.findElement(By.xpath("//*[text()='Story Arc']")).click();

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 1')]"));
    documentFeature(
        "Booker",
        "Story Arc Wizard",
        "Three-step wizard for scripting a feud arc: select wrestlers, name the arc and choose"
            + " its length, then add ordered beats (match type, stipulation, winner control).",
        "booker-story-arc-wizard");
  }
}

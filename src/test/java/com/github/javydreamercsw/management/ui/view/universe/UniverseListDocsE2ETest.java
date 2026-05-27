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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class UniverseListDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureUniverseListScreenshot() {
    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    documentFeature(
        "Admin",
        "Universe Management",
        "Universes are the top-level containers for all promotion data — shows, wrestlers,"
            + " titles, factions, and storylines all belong to a universe. An installation"
            + " can run multiple universes simultaneously, each with its own independent"
            + " roster, calendar, and championship lineage.",
        "universe-list-overview");
  }

  @Test
  void testRecordUniverseListWalkthrough() {
    setVideoInfo("Admin", "Universe Management Walkthrough", "universe-list-walkthrough");

    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    captureCaption(
        "Universe List — every universe in the installation is listed here with its name,"
            + " type, and creation date. Each universe is a fully isolated promotion sandbox:"
            + " wrestlers, shows, and titles in one universe don't bleed into another.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(800);

    captureCaption(
        "Use Edit to rename a universe or change its type. Delete removes the universe and"
            + " all associated data permanently — this action requires confirmation and"
            + " cannot be undone.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(600);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement createButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Create Universe')]")));
    captureCaption(
        "Click Create Universe to define a new promotion sandbox. Give it a name and"
            + " type (Standard, Fantasy, or Historical), then populate it with wrestlers"
            + " and start booking shows independently of your other universes.",
        4500);
    clickElement(createButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(600);
    captureCaption(
        "Create Universe dialog — name is required; type controls which game rules apply."
            + " Standard uses the default ATW ruleset; Fantasy enables league drafts;"
            + " Historical locks the roster to a specific era.",
        4000);

    WebElement cancelButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//vaadin-button[text()='Cancel']")));
    clickElement(cancelButton);

    sleep(1500);
  }
}

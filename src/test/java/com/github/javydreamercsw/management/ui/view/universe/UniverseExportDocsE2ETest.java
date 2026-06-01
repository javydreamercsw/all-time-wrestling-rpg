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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

class UniverseExportDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureExportDataDialog() {
    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement exportButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Export Data')]")));
    clickElement(exportButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(600);

    documentFeature(
        "Entities",
        "Export Universe Data",
        "Export wrestler state, injuries, rivalries, title reigns, alignments and relationships"
            + " to CSV or JSON",
        "export-universe-data");
  }
}

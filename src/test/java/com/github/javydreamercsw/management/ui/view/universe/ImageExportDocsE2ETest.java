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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class ImageExportDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureImageExportButton() {
    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    waitForVaadinElement(driver, By.id("export-images-button"));

    sleep(500);

    documentFeature(
        "Admin",
        "Export Custom Images",
        "Download a ZIP of all custom images (wrestlers, factions, titles, arenas, and more)"
            + " in one click. The archive preserves the original file paths so images can be"
            + " restored on a new host without reconfiguration.",
        "export-universe-images");
  }
}

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
package com.github.javydreamercsw.management.ui.view.season;

import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

class SeasonDetailViewDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void captureSeasonListWithDetailsLink() {
    navigateTo("season-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    documentFeature(
        "Booker",
        "Season List",
        "Browse all seasons with their status, show count, and date range."
            + " Click 'Details' on any season to open its full detail page,"
            + " including the awards ceremony results.",
        "booker-season-list");
  }

  @Test
  void captureSeasonDetailView() {
    navigateTo("season-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    var detailLinks = driver.findElements(By.partialLinkText("Details"));
    if (!detailLinks.isEmpty()) {
      detailLinks.get(0).click();
      waitForVaadinClientToLoad();
      waitForVaadinElement(driver, By.xpath("//*[contains(., 'Season Overview')]"));
      documentFeature(
          "Booker",
          "Season Detail",
          "Full season breakdown showing key stats (status, show count, duration)"
              + " and the Awards Ceremony section listing Wrestler of the Year,"
              + " Most Improved, and Most Decorated winners.",
          "booker-season-detail");
    }
  }
}

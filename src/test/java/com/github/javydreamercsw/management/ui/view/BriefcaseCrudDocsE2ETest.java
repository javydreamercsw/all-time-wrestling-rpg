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

import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs capture of the briefcase CRUD management view (ATW-jpki): the list with holder, status
 * badges and the Edit/Void actions on live (HELD) cases.
 */
class BriefcaseCrudDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TitleOpportunityService titleOpportunityService;

  @Test
  void captureBriefcaseCrudView() throws InterruptedException {
    navigateTo("briefcase-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    documentFeature(
        "Game Mechanics",
        "Briefcase Management",
        "Bookers manage the Money in the Bank-style briefcases from the Entities menu: every"
            + " case with its holder, division, status and cashable-until date. Live (HELD)"
            + " cases can be edited — rename, override the expiry, upload artwork — or voided"
            + " (a manual cancel that keeps the row as history). Cashed-in and expired rows are"
            + " read-only history.",
        "mechanic-briefcase-crud");
    Thread.sleep(500);
  }
}

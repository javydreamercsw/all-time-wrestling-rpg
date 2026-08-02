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
package com.github.javydreamercsw.management.ui.view.home;

import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

class HomeDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void testCaptureHomeView() {
    navigateTo("");
    waitForVaadinElement(driver, By.xpath("//*[contains(@class,'home-view')]"));
    documentFeature(
        "Dashboards",
        "Home Dashboard",
        "The home page shows a personalised welcome, a summary of what changed since your last"
            + " login (inbox items, universe stats), and the latest wrestling news and rumours.",
        "dashboard-home");
  }
}

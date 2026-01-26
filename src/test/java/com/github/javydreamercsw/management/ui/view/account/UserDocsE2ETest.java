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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;

public class UserDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void testCaptureProfileDrawer() {
    login("player", "player123");

    // Open Profile Drawer
    clickButtonByText("Profile");
    waitForVaadinClientToLoad();

    // Wait for drawer content
    waitForText("Profile & Settings");
    waitForText("Theme");

    documentFeature(
        "User Settings",
        "Profile Drawer",
        "Access your personal settings by clicking the Profile button in the top navigation bar."
            + " This opens a dedicated drawer on the right side of the screen, allowing you to"
            + " manage your account without leaving your current context.",
        "profile-drawer");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

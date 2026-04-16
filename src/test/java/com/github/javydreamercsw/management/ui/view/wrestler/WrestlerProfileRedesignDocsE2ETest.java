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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WrestlerProfileRedesignDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureRedesignedProfile() {
    Wrestler wrestler =
        wrestlerRepository
            .findByName("Johnny All Time")
            .orElseThrow(() -> new RuntimeException("Johnny All Time not found"));

    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler.getId());
    waitForVaadinClientToLoad();

    waitForText("Biography");
    waitForText("Career Stats");

    documentFeature(
        "Wrestler Profile",
        "Modernized Wrestler Dashboard",
        "The redesigned wrestler profile features a prominent 'Hero' section with the wrestler's"
            + " image, biography, and personal relationships always visible. Secondary information"
            + " is organized into a clean, mobile-friendly accordion layout.",
        "wrestler-profile-redesign");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

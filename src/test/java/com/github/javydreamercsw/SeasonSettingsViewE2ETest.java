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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

public class SeasonSettingsViewE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private TierBoundaryService tierBoundaryService;
  @Autowired private TierBoundaryRepository tierBoundaryRepository;

  @BeforeEach
  public void setupBoundaries() {
    // Reset boundaries to a known state first
    tierBoundaryService.resetTierBoundaries();
  }

  @Test
  void testResetTierBoundaries() {
    // Change a boundary to something different
    TierBoundary boundary =
        tierBoundaryRepository.findAllByGender(Gender.MALE).stream()
            .filter(b -> b.getTier().equals(WrestlerTier.MAIN_EVENTER))
            .findFirst()
            .get();

    long originalMinFans = boundary.getMinFans();
    boundary.setMinFans(originalMinFans + 100);
    tierBoundaryService.save(boundary);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/season/settings");

    // Wait for view to load
    WebElement resetBoundariesButton =
        waitForVaadinElement(driver, By.id("reset-boundaries-button"));
    clickElement(resetBoundariesButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    assertTrue(dialog.isDisplayed());

    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-reset-boundaries-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Tier boundaries reset successfully.", notification.getText());

    // Verify boundaries were reset
    WrestlerTier tier = boundary.getTier();
    TierBoundary updatedBoundary =
        tierBoundaryRepository.findAllByGender(Gender.MALE).stream()
            .filter(b -> b.getTier().equals(WrestlerTier.MAIN_EVENTER))
            .findFirst()
            .get();

    assertEquals(tier.getMinFans(), updatedBoundary.getMinFans());
  }

  @Test
  void testResetFans() {
    // Make sure there is at least one wrestler with fans > minFans
    Wrestler wrestler = wrestlerService.findAll().get(0);
    WrestlerTier tier = wrestler.getTier();
    wrestler.setFans(tier.getMinFans() + 100);
    wrestlerService.save(wrestler);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/season/settings");

    // Wait for view to load
    WebElement resetFansButton = waitForVaadinElement(driver, By.id("reset-fans-button"));
    clickElement(resetFansButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    assertTrue(dialog.isDisplayed());

    WebElement confirmButton = waitForVaadinElement(driver, By.id("confirm-reset-fans-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Fan counts reset successfully.", notification.getText());

    // Verify fan counts were reset
    Wrestler updatedWrestler = wrestlerService.findById(wrestler.getId()).get();
    assertEquals(tier.getMinFans(), updatedWrestler.getFans());
  }
}

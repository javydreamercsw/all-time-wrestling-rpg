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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
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
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;

  private Universe defaultUniverse;

  @BeforeEach
  public void setupBoundaries() {
    // Reset boundaries to a known state first
    tierBoundaryService.resetTierBoundaries();

    defaultUniverse =
        universeRepository
            .findById(1L)
            .orElseGet(
                () ->
                    universeRepository.save(
                        Universe.builder().id(1L).name("Default Universe").build()));
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
    tierBoundaryRepository.saveAndFlush(boundary);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement resetBoundariesButton =
        waitForVaadinElement(driver, By.id("reset-boundaries-button"));
    clickElement(resetBoundariesButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
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
  void testRecalibrateFans() {
    // Make sure there is at least one wrestler
    Wrestler wrestler =
        wrestlerService.findAll().stream()
            .findFirst()
            .orElseGet(() -> wrestlerService.createWrestler("Recalibrate Test", false, null));

    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    WrestlerTier tier = state.getTier();
    state.setFans(tier.getMinFans() + 100);
    wrestlerStateRepository.saveAndFlush(state);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement recalibrateFansButton =
        waitForVaadinElement(driver, By.id("recalibrate-fans-button"));
    clickElement(recalibrateFansButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());

    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-recalibrate-fans-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Fan counts recalibrated successfully.", notification.getText());

    // Verify fan counts were reset
    WrestlerState updatedState =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    assertEquals(tier.getMinFans(), updatedState.getFans());
  }

  @Test
  void testRecalibrateIconFans() {
    // Create an Icon wrestler
    Wrestler icon =
        wrestlerService.createWrestler(
            "Test Icon", false, "Test Icon", WrestlerTier.ICON, defaultUniverse);
    WrestlerState state = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    state.setFans(WrestlerTier.ICON.getMinFans());
    wrestlerStateRepository.saveAndFlush(state);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement recalibrateFansButton =
        waitForVaadinElement(driver, By.id("recalibrate-fans-button"));
    clickElement(recalibrateFansButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());

    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-recalibrate-fans-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Fan counts recalibrated successfully.", notification.getText());

    // Verify fan counts were reset and tier was demoted
    WrestlerState updatedState =
        wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    assertEquals(WrestlerTier.MAIN_EVENTER, updatedState.getTier());
    assertEquals(WrestlerTier.MAIN_EVENTER.getMinFans(), updatedState.getFans());
  }

  @Test
  void testResetFans() {
    // Create an Icon wrestler
    Wrestler icon =
        wrestlerService.createWrestler(
            "Test Icon", false, "Test Icon", WrestlerTier.ICON, defaultUniverse);
    WrestlerState state = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    state.setFans(WrestlerTier.ICON.getMinFans());
    wrestlerStateRepository.saveAndFlush(state);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement recalibrateFansButton = waitForVaadinElement(driver, By.id("full-reset-button"));
    clickElement(recalibrateFansButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());

    WebElement confirmButton = waitForVaadinElement(driver, By.id("confirm-full-reset-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("All wrestler fan counts have been reset to 0.", notification.getText());

    // Verify fan counts were reset and tier was demoted
    WrestlerState updatedState =
        wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    assertEquals(WrestlerTier.ROOKIE, updatedState.getTier());
    assertEquals(0L, updatedState.getFans());
  }
}

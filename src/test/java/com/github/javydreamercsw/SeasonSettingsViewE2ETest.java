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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
public class SeasonSettingsViewE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private TierBoundaryService tierBoundaryService;
  @Autowired private TierBoundaryRepository tierBoundaryRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;

  @BeforeEach
  public void setupBoundaries() {
    // Reset boundaries to a known state first
    tierBoundaryService.resetTierBoundaries();
  }

  @Test
  void testResetTierBoundaries() {
    setVideoInfo("Season Management", "Reset Tier Boundaries", "reset-tier-boundaries");

    // Change a boundary to something different
    TierBoundary boundary =
        tierBoundaryRepository.findAllByGender(Gender.MALE).stream()
            .filter(b -> b.getTier().equals(WrestlerTier.MAIN_EVENTER))
            .findFirst()
            .get();

    long originalMinFans = boundary.getMinFans();
    boundary.setMinFans(originalMinFans + 100);
    tierBoundaryRepository.saveAndFlush(boundary);

    navigateTo("admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement resetBoundariesButton =
        waitForVaadinElement(driver, By.id("reset-boundaries-button"));
    captureCaption(
        "Season Settings — tier boundaries define the minimum fan count required for each"
            + " wrestler tier (Rookie through Icon). Use Reset Tier Boundaries to restore"
            + " the defaults if boundaries were customised during the season.",
        4000);
    clickElement(resetBoundariesButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());
    captureCaption(
        "Confirm to restore all tier fan thresholds to their default values."
            + " This does not change any wrestler's current fan count — only the boundary"
            + " targets are reset.",
        3500);

    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-reset-boundaries-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Tier boundaries reset successfully.", notification.getText());
    captureCaption(
        "Tier boundaries reset — all tiers now use their default fan requirements,"
            + " ready for the new season. Wrestlers are automatically re-evaluated against"
            + " the restored thresholds on the next ranking calculation.",
        3500);

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
    setVideoInfo("Season Management", "Recalibrate Fan Counts", "recalibrate-fans");

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

    navigateTo("admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement recalibrateFansButton =
        waitForVaadinElement(driver, By.id("recalibrate-fans-button"));
    captureCaption(
        "Recalibrate Fans trims each wrestler's fan count to the minimum for their current"
            + " tier — useful for resetting momentum at the start of a new season without"
            + " demoting anyone or wiping tiers entirely.",
        4000);
    clickElement(recalibrateFansButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());
    captureCaption(
        "Confirm to recalibrate fan counts across all wrestlers. Icon-tier wrestlers are"
            + " demoted to Main Eventer automatically since Icon has no upper boundary.",
        3500);

    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-recalibrate-fans-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("Fan counts recalibrated successfully.", notification.getText());
    captureCaption(
        "Fan counts recalibrated — wrestlers retain their tier but excess fans are"
            + " trimmed to the tier minimum, levelling the playing field. This creates"
            + " a competitive reset without erasing the hierarchy built during the season.",
        4000);

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

    navigateTo("admin");

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
    assertEquals(WrestlerTier.ICON, updatedState.getTier());
    assertEquals(WrestlerTier.MAIN_EVENTER.getMinFans(), updatedState.getFans());
  }

  @Test
  void testResetFans() {
    setVideoInfo("Season Management", "Full Fan Reset", "full-fan-reset");

    // Create an Icon wrestler
    Wrestler icon =
        wrestlerService.createWrestler(
            "Test Icon", false, "Test Icon", WrestlerTier.ICON, defaultUniverse);
    WrestlerState state = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    state.setFans(WrestlerTier.ICON.getMinFans());
    wrestlerStateRepository.saveAndFlush(state);

    navigateTo("admin");

    // Click the tab
    click("vaadin-tab", "Season Settings");

    // Wait for view to load
    WebElement fullResetButton = waitForVaadinElement(driver, By.id("full-reset-button"));
    captureCaption(
        "Full Fan Reset wipes all wrestler fan counts to zero and demotes every wrestler"
            + " back to Rookie — a complete season restart. Use this only when you want"
            + " every competitor to rebuild their career from scratch.",
        4500);
    clickElement(fullResetButton);

    // Wait for dialog
    WebElement dialog = waitForVaadinElement(driver, By.tagName("vaadin-dialog"));
    assertTrue(dialog.isDisplayed());
    captureCaption(
        "This action is irreversible — confirm only when you want to start a brand-new"
            + " season from scratch. All fan counts and tier standings will be permanently"
            + " reset to zero.",
        4000);

    WebElement confirmButton = waitForVaadinElement(driver, By.id("confirm-full-reset-button"));
    clickElement(confirmButton);

    // Wait for notification
    WebElement notification = waitForVaadinElement(driver, By.tagName("vaadin-notification-card"));
    assertEquals("All wrestler fan counts have been reset to 0.", notification.getText());
    captureCaption(
        "All fan counts reset to 0 and tiers demoted to Rookie — the new season starts"
            + " with a clean slate. Every wrestler must earn their way back up the card"
            + " through wins, promos, and fan-building events.",
        4000);

    // Verify fan counts were reset and tier was demoted
    WrestlerState updatedState =
        wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    assertEquals(WrestlerTier.ROOKIE, updatedState.getTier());
    assertEquals(0L, updatedState.getFans());
  }
}

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
package com.github.javydreamercsw.management.ui.view.tutorial;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;

class TutorialDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TutorialService tutorialService;
  @Autowired private AccountRepository accountRepository;

  @Override
  protected String getUsername() {
    return "player";
  }

  @Override
  protected String getPassword() {
    return "player123";
  }

  @BeforeEach
  void resetTutorialState() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.markIncomplete(account.getId(), Universe.UniverseType.GLOBAL));
  }

  @Test
  void playerTutorialStep1() {
    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Global Tutorial — Step 1",
        "The tutorial guides new players through the key actions for their mode. Step 1 asks the"
            + " player to pick their featured wrestler.",
        "tutorial-global-step1");
  }

  @Test
  void playerTutorialStep2Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 1, 5));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Global Tutorial — Step 2",
        "Step 2 guides the player to create their first show in the universe.",
        "tutorial-global-step2");
  }

  @Test
  void playerTutorialStep3Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 2, 5));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Global Tutorial — Step 3",
        "Step 3 asks the player to plan their card by adding at least one segment to their show.",
        "tutorial-global-step3");
  }

  @Test
  void playerTutorialStep4Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 3, 5));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Global Tutorial — Step 4",
        "Step 4 asks the player to run their show by adjudicating its segments.",
        "tutorial-global-step4");
  }

  @Test
  void playerTutorialStep5Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 4, 5));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Global Tutorial — Step 5",
        "Step 5 introduces AI narration — players can configure an AI provider to generate"
            + " match commentary.",
        "tutorial-global-step5");
  }

  @Test
  void playerTutorialSkipDismissesAndDoesNotRedirectAgain() {
    // Pre-create the tutorial universe so the wizard (with Skip button) shows, not mode-selection.
    // markIncomplete() in @BeforeEach deletes the completion record but not the universe.
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.createTutorialUniverse(
                    account, Universe.UniverseType.GLOBAL, Map.of()));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    assertThat(driver.getCurrentUrl()).contains("tutorial");

    // Click Skip Tutorial button
    try {
      driver.findElement(By.xpath("//vaadin-button[contains(., 'Skip Tutorial')]")).click();
    } catch (NoSuchElementException e) {
      // Try standard button
      driver.findElement(By.xpath("//button[contains(., 'Skip Tutorial')]")).click();
    }
    waitForVaadinClientToLoad();

    // Should have navigated away from tutorial
    assertThat(driver.getCurrentUrl()).doesNotContain("tutorial");

    // Navigate away and back — should NOT redirect to tutorial again
    navigateTo("player");
    waitForVaadinClientToLoad();
    assertThat(driver.getCurrentUrl()).doesNotContain("tutorial");
  }
}

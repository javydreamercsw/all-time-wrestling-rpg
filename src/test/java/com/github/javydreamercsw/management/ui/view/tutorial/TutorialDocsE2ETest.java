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
        "Players",
        "Tutorial — Step 1",
        "The tutorial guides new players through the key actions for their mode. Step 1 asks the"
            + " player to assign a wrestler on their Player Dashboard.",
        "players-tutorial-step1");
  }

  @Test
  void playerTutorialStep2Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account -> {
              tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 1, 3);
            });

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Players",
        "Tutorial — Step 2",
        "Step 2 guides the player to create their first show in the universe.",
        "players-tutorial-step2");
  }

  @Test
  void playerTutorialStep3Navigation() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account -> {
              tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 2, 3);
            });

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Players",
        "Tutorial — Step 3",
        "Step 3 asks the player to run their first show by adjudicating its segments.",
        "players-tutorial-step3");
  }

  @Test
  void playerTutorialSkipDismissesAndDoesNotRedirectAgain() {
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

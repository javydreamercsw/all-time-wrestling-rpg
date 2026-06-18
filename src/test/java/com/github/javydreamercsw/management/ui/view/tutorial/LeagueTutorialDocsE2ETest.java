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

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LeagueTutorialDocsE2ETest extends AbstractDocsE2ETest {

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
                tutorialService.markIncomplete(account.getId(), Universe.UniverseType.LEAGUE));
  }

  @Test
  void leagueTutorialStep1() {
    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "League Tutorial — Step 1",
        "The league tutorial begins by creating a tutorial league and making the player the"
            + " commissioner. Step 1 shows the Leagues page with the player's tutorial league.",
        "tutorial-league-step1");
  }

  @Test
  void leagueTutorialStep2() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 1, 4));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "League Tutorial — Step 2",
        "Step 2 guides the commissioner to invite other players to their league by generating an"
            + " invite link.",
        "tutorial-league-step2");
  }

  @Test
  void leagueTutorialStep3() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 2, 4));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "League Tutorial — Step 3",
        "Step 3 instructs the commissioner to start the snake draft once all players have joined.",
        "tutorial-league-step3");
  }

  @Test
  void leagueTutorialStep4() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 3, 4));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "League Tutorial — Step 4",
        "Step 4 takes the player to the Draft page to make their first wrestler pick, completing"
            + " the league setup tutorial.",
        "tutorial-league-step4");
  }
}

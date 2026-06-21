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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class TutorialVideoDocsE2ETest extends AbstractE2ETest {

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

  @Test
  void globalTutorialWalkthrough() {
    setVideoInfo("Tutorial", "Global Tutorial Walkthrough", "tutorial-global-walkthrough");

    tutorialService.markIncomplete(
        accountRepository.findByUsername("player").orElseThrow().getId(),
        Universe.UniverseType.GLOBAL);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.createTutorialUniverse(
                    account, Universe.UniverseType.GLOBAL, Map.of()));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 1 — Pick Your Featured Wrestler", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 1, 5));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 2 — Create Your First Show", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 2, 5));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 3 — Plan Your Card", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 3, 5));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 4 — Run Your Show", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.GLOBAL, 4, 5));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 5 — Enhance with AI", 2000);
  }

  @Test
  void leagueTutorialWalkthrough() {
    setVideoInfo("Tutorial", "League Tutorial Walkthrough", "tutorial-league-walkthrough");

    tutorialService.markIncomplete(
        accountRepository.findByUsername("player").orElseThrow().getId(),
        Universe.UniverseType.LEAGUE);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.createTutorialUniverse(
                    account, Universe.UniverseType.LEAGUE, Map.of()));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 1 — Your Tutorial League", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 1, 4));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 2 — Invite Players", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 2, 4));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 3 — Start the Draft", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.LEAGUE, 3, 4));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 4 — Make Your First Draft Pick", 2000);
  }

  @Test
  void campaignTutorialWalkthrough() {
    setVideoInfo("Tutorial", "Campaign Tutorial Walkthrough", "tutorial-campaign-walkthrough");

    tutorialService.markIncomplete(
        accountRepository.findByUsername("player").orElseThrow().getId(),
        Universe.UniverseType.CAMPAIGN);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.createTutorialUniverse(
                    account, Universe.UniverseType.CAMPAIGN, Map.of()));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 1 — Assign Your Wrestler", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.CAMPAIGN, 1, 3));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 2 — Start Your Campaign", 2000);

    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.CAMPAIGN, 2, 3));

    navigateTo("tutorial");
    waitForVaadinClientToLoad();
    captureCaption("Step 3 — Make Your First Backstage Action", 2000);
  }
}

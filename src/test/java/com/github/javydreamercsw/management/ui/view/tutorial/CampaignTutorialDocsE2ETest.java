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

class CampaignTutorialDocsE2ETest extends AbstractDocsE2ETest {

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
                tutorialService.markIncomplete(account.getId(), Universe.UniverseType.CAMPAIGN));
  }

  @Test
  void campaignTutorialStep1() {
    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Campaign Tutorial — Step 1",
        "The campaign tutorial starts by asking the player to assign a wrestler on their Player"
            + " Dashboard. Only wrestlers allowed by the first campaign chapter are shown.",
        "tutorial-campaign-step1");
  }

  @Test
  void campaignTutorialStep2() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.CAMPAIGN, 1, 3));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Campaign Tutorial — Step 2",
        "Step 2 directs the player to the Campaign view where an active campaign has been"
            + " automatically created for their chosen wrestler.",
        "tutorial-campaign-step2");
  }

  @Test
  void campaignTutorialStep3() {
    accountRepository
        .findByUsername("player")
        .ifPresent(
            account ->
                tutorialService.advanceStep(account.getId(), Universe.UniverseType.CAMPAIGN, 2, 3));

    login("player", "player123");
    navigateTo("tutorial");
    waitForVaadinClientToLoad();

    documentFeature(
        "Tutorial",
        "Campaign Tutorial — Step 3",
        "Step 3 sends the player to the Backstage Actions page to make their first story-driven"
            + " choice, completing the campaign tutorial.",
        "tutorial-campaign-step3");
  }
}

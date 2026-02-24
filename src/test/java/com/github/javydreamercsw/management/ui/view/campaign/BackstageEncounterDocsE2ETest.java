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
package com.github.javydreamercsw.management.ui.view.campaign;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class BackstageEncounterDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private BackstageEncounterService backstageEncounterService;
  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    if (!campaignService.hasActiveCampaign(player)) {
      campaignService.startCampaign(player);
    }

    // Clear any previous encounter data for today
    Campaign campaign = campaignRepository.findActiveByWrestler(player).get();
    campaign.getState().setFeatureData(null);
    campaign.getState().setActionsTaken(0);
    campaignRepository.saveAndFlush(campaign);
  }

  private Wrestler getOrCreateWrestler(Account account) {
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    if (!wrestlers.isEmpty()) {
      return wrestlers.get(0);
    }

    Wrestler w =
        Wrestler.builder()
            .name("Docs Wrestler")
            .startingHealth(100)
            .startingStamina(100)
            .account(account)
            .isPlayer(true)
            .active(true)
            .gender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE)
            .build();
    return wrestlerRepository.saveAndFlush(w);
  }

  @Test
  void testCaptureBackstageEncounter() {
    // 1. Force the random trigger to succeed
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextInt(100)).thenReturn(0); // 0 < 20
    ReflectionTestUtils.setField(backstageEncounterService, "random", mockRandom);

    // 2. Navigate to Backstage Actions - should trigger reroute
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign/actions");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture Situation
    waitForText("Backstage Situation");
    documentFeature(
        "Campaign",
        "Backstage Encounters",
        "Random encounters can occur when you visit the backstage area. These interactive"
            + " dialogue segments force you to make choices that affect your alignment and"
            + " momentum.",
        "campaign-backstage-encounter");

    // 4. Click a choice (Respect)
    WebElement respectBtn = waitForVaadinElement(driver, By.id("backstage-choice-respect"));
    clickElement(respectBtn);

    // 5. Verify & Capture Outcome
    waitForText("Outcome:");
    documentFeature(
        "Campaign",
        "Encounter Outcomes",
        "Every choice in a backstage encounter has a narrative outcome and mechanical"
            + " consequences, such as alignment shifts or bonuses for your next match.",
        "campaign-backstage-encounter-outcome");
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

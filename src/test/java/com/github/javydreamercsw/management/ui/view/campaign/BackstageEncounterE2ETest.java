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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class BackstageEncounterE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @MockitoBean private BackstageEncounterService backstageEncounterService;

  @BeforeEach
  void setup() {
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
            .name("Test E2E Wrestler")
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
  void testRandomEncounterTriggerAndFlow() {
    // 1. Force the random trigger to succeed
    when(backstageEncounterService.shouldTriggerEncounter(any())).thenReturn(true);

    // Provide a mock encounter when requested
    var choice1 =
        com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO.Choice
            .builder()
            .text("Option A")
            .label("Respect")
            .alignmentShift(1)
            .momentumBonus(1)
            .outcomeText("The veteran nods in approval.")
            .nextPhase("BACKSTAGE")
            .build();

    var response =
        new com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO(
            "Mock Situation: You are approached by a veteran", java.util.List.of(choice1));

    when(backstageEncounterService.generateBackstageEncounter(any())).thenReturn(response);

    // Mock recordBackstageChoice to do nothing (it's a void method)
    // and avoid it failing because of missing DB records in the mock context
    doNothing().when(backstageEncounterService).recordBackstageChoice(any(), any());

    // 2. Navigate to Backstage Actions - should trigger reroute
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign/actions");
    waitForVaadinClientToLoad();

    // 3. Verify we are on the situation view
    waitForText("Backstage Situation");
    assertTrue(driver.getCurrentUrl().contains("backstage-situation"));

    // Check mock narrative
    waitForText("Mock Situation: You are approached by a veteran");

    // 4. Click a choice (Respect)
    WebElement respectBtn = waitForVaadinElement(driver, By.id("backstage-choice-respect"));
    clickElement(respectBtn);

    // 5. Verify outcome
    waitForText("Outcome:");
    waitForText("The veteran nods in approval");

    // 6. Finish interaction
    WebElement finishBtn = waitForVaadinElement(driver, By.id("finish-backstage-situation-button"));
    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("arguments[0].click();", finishBtn);
    waitForVaadinClientToLoad();

    // 7. Verify we are back in Backstage Actions
    // The view should show "Backstage Area" title (it's an H2)
    waitForVaadinElement(driver, By.xpath("//h2[text()='Backstage Area']"));
    assertTrue(driver.getCurrentUrl().contains("campaign/actions"));
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

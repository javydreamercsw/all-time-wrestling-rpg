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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

class CampaignPromoE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;

  private Wrestler player;

  @BeforeEach
  void setupCampaign() {
    // Manually setup a wrestler for the admin account to avoid brittle UI steps
    Account admin = accountRepository.findByUsername("admin").get();

    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    player = wrestlers.isEmpty() ? null : wrestlers.getFirst();

    if (player == null) {
      Wrestler w =
          Wrestler.builder()
              .name("Test E2E Wrestler")
              .startingHealth(100)
              .startingStamina(100)
              .account(admin)
              .isPlayer(true)
              .active(true)
              .build();
      player = wrestlerRepository.save(w);
    }

    if (!campaignService.hasActiveCampaign(player)) {
      campaignService.startCampaign(player);
    }
  }

  @Test
  void testPromoFlow_MockAI() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // 1. Navigate directly to Promo View
    // URL pattern: campaign/promo
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign/promo");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("promo-view-initial");

    // 2. Verify we are on the promo view
    waitForText("Cutting a Promo");

    // 3. Wait for the mock content to appear
    // The mock opener starts with "The crowd is buzzing"
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("narrative-container"), "The crowd is buzzing"));
    takeSequencedScreenshot("promo-view-loaded");

    // 4. Verify mock hook is present (Cheap Heat)
    WebElement hookButton = waitForVaadinElement(driver, By.id("promo-hook-cheap-heat"));

    // 5. Click the hook
    clickElement(hookButton);
    waitForVaadinClientToLoad();

    // 6. Verify outcome
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("narrative-container"), "Promo SUCCESSFUL"));

    waitForText("SUCCESSFUL");
    takeSequencedScreenshot("promo-outcome");

    // 7. Click Finish
    WebElement finishButton = waitForVaadinElement(driver, By.id("finish-promo-button"));
    clickElement(finishButton);
    waitForVaadinClientToLoad();

    // 8. Verify navigation back to actions (or wherever finish leads)
    // The finish button navigates to "campaign/actions"
    assertTrue(driver.getCurrentUrl().contains("campaign/actions"));
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

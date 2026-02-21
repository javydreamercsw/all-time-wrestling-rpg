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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class CampaignPromoE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository
      campaignStateRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository
      backstageActionHistoryRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository
      campaignEncounterRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository
      wrestlerAlignmentRepository;

  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @BeforeEach
  void setupCampaign() {
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();

    dataInitializer.init();

    // Manually set up a wrestler for the admin account to avoid brittle UI steps
    Account admin = accountRepository.findByUsername("admin").get();

    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    Wrestler player = wrestlers.isEmpty() ? null : wrestlers.getFirst();

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
    log.info("Waiting for hook button...");
    WebElement hookButton = waitForVaadinElement(driver, By.id("promo-hook-cheap-heat"));

    // 5. Click the hook
    log.info("Clicking hook button...");
    clickElement(hookButton);

    // Wait for the UI to acknowledge the click by showing the processing text
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("narrative-container"), "Processing choice"));

    // Wait for async processing: first wait for progress bar to appear, then disappear
    log.info("Waiting for progress bar to appear...");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("promo-progress-bar")));

    log.info("Waiting for promo processing to complete...");
    new WebDriverWait(driver, Duration.ofSeconds(120))
        .until(ExpectedConditions.invisibilityOfElementLocated(By.id("promo-progress-bar")));

    waitForVaadinClientToLoad();

    // 6. Verify outcome
    log.info("Waiting for outcome text...");
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("narrative-container"), "Promo SUCCESSFUL"));

    waitForText("SUCCESSFUL");
    log.info("Outcome verified.");
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

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
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
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

class SmartPromoE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired private CampaignEncounterRepository campaignEncounterRepository;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @BeforeEach
  void setupCampaign() {
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();

    dataInitializer.init();

    Account admin = accountRepository.findByUsername("admin").get();

    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
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
    } else {
      // Ensure alignment exists if campaign already exists (since we deleted it earlier)
      final Wrestler finalPlayer = player;
      wrestlerAlignmentRepository
          .findByWrestler(player)
          .orElseGet(
              () -> {
                com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment
                    newAlignment =
                        com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment
                            .builder()
                            .wrestler(finalPlayer)
                            .alignmentType(
                                com.github.javydreamercsw.management.domain.campaign.AlignmentType
                                    .NEUTRAL)
                            .level(0)
                            .campaign(campaignRepository.findActiveByWrestler(finalPlayer).get())
                            .build();
                return wrestlerAlignmentRepository.save(newAlignment);
              });
    }

    // Force unlock promo and ensure we are in BACKSTAGE phase
    campaignRepository
        .findActiveByWrestler(player)
        .ifPresent(
            campaign -> {
              CampaignState state = campaign.getState();
              state.setPromoUnlocked(true);
              state.setCurrentPhase(
                  com.github.javydreamercsw.management.domain.campaign.CampaignPhase.BACKSTAGE);
              state.setActionsTaken(0);
              campaignStateRepository.save(state);
            });
  }

  @Test
  void testSmartPromoFlow() {
    // 1. Navigate to Backstage Actions
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign/actions");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("smart-promo-actions-initial");

    // 2. Click Promo action
    WebElement promoButton = waitForVaadinElement(driver, By.id("action-button-PROMO-INTERACTIVE"));
    clickElement(promoButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("smart-promo-view-initial");

    // 3. Verify we are on PromoView
    waitForVaadinElement(driver, By.id("promo-view-title"));
    assertTrue(driver.getPageSource().contains("Cutting a Promo"));

    // 4. Wait for AI content (Opener and Hooks)
    waitForVaadinElement(driver, By.id("narrative-container"));
    waitForVaadinElement(driver, By.id("promo-hook-cheap-heat"));

    // 5. Select a hook
    WebElement hookBtn = waitForVaadinElement(driver, By.id("promo-hook-cheap-heat"));
    clickElement(hookBtn);
    takeSequencedScreenshot("smart-promo-processing-hook");

    // Wait for async processing: first wait for progress bar to appear, then disappear
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.visibilityOfElementLocated(By.id("promo-progress-bar")));

    new WebDriverWait(driver, Duration.ofSeconds(120))
        .until(ExpectedConditions.invisibilityOfElementLocated(By.id("promo-progress-bar")));

    // Give Vaadin time to push UI updates to client
    waitForVaadinClientToLoad();

    // 6. Wait for outcome
    waitForVaadinElement(driver, By.id("promo-outcome-status"));
    assertTrue(
        driver.getPageSource().contains("SUCCESSFUL") || driver.getPageSource().contains("FAILED"));
    takeSequencedScreenshot("smart-promo-outcome");

    // 7. Finish Promo
    WebElement finishBtn = waitForVaadinElement(driver, By.id("finish-promo-button"));
    clickElement(finishBtn);
    waitForVaadinClientToLoad();

    // 8. Verify back on actions view and action count increased
    waitForText("Actions taken today: 1 / 2");
    takeSequencedScreenshot("smart-promo-back-to-actions");
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
@Slf4j
class CampaignListVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testRecordCampaignListWalkthrough() {
    setVideoInfo("Campaign", "Campaign List Walkthrough", "campaign-list-walkthrough");

    // 1. Seed a campaign so the grid is non-empty
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin, "Video Wrestler");
    createCampaignInChapter(player, "beginning");

    // 2. Navigate to the campaign list
    navigateTo("campaign-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    captureCaption(
        "Campaign List — an admin-only view showing every campaign running in the current"
            + " universe. Each row displays the wrestler's name, campaign status, and the"
            + " date the campaign started.",
        4500);

    // 3. Scroll down to show the full grid
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    waitForVaadinClientToLoad();
    sleep(1500);
    captureCaption(
        "Campaigns are automatically scoped to the active universe — switching universes"
            + " in the top toolbar refreshes the list instantly, so admins can manage"
            + " separate rosters without overlap.",
        4000);

    // 4. Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);

    // 5. Click the Dashboard button on the first campaign row
    captureCaption(
        "Click the Dashboard button on any row to jump directly to that wrestler's campaign"
            + " dashboard — the same view the player uses to run their campaign matches"
            + " and backstage actions.",
        3500);

    try {
      WebElement dashboardBtn =
          driver.findElement(By.cssSelector("[id^='campaign-dashboard-btn-']"));
      clickElement(dashboardBtn);
      waitForVaadinClientToLoad();
      captureCaption(
          "The Campaign Dashboard shows the wrestler's current chapter, alignment track,"
              + " win/loss record, and available actions — giving admins full visibility"
              + " into any player's campaign state.",
          4500);
    } catch (Exception e) {
      // Dashboard button may not be present if the grid is empty — that's OK for a video seed
      log.warn("Dashboard button not found during video recording: {}", e.getMessage());
    }

    sleep(2000);
  }

  private Wrestler getOrCreateWrestler(@NonNull final Account account, @NonNull final String name) {
    return wrestlerRepository.findByAccount(account).stream()
        .filter(w -> name.equals(w.getName()))
        .findFirst()
        .orElseGet(
            () -> {
              Wrestler w =
                  Wrestler.builder()
                      .name(name)
                      .startingHealth(100)
                      .startingStamina(100)
                      .account(account)
                      .isPlayer(true)
                      .active(true)
                      .gender(Gender.MALE)
                      .build();
              return wrestlerRepository.saveAndFlush(w);
            });
  }

  private Campaign createCampaignInChapter(
      @NonNull final Wrestler player, @NonNull final String chapterId) {
    if (campaignService.hasActiveCampaign(player)) {
      Campaign existing = campaignRepository.findActiveByWrestler(player).get();
      existing.getState().setCurrentChapterId(chapterId);
      return campaignRepository.save(existing);
    }
    Campaign c = campaignService.startCampaign(player);
    c.getState().setCurrentChapterId(chapterId);
    return campaignRepository.save(c);
  }
}

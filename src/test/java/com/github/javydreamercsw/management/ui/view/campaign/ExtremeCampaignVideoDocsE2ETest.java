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
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
@Slf4j
class ExtremeCampaignVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testRecordExtremeCampaignChapterPickerWalkthrough() {
    setVideoInfo("Campaign", "Starting an Extreme Campaign", "campaign-extreme-chapter-picker");

    // 1. Ensure a clean state — no active campaign for the player
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin, "Extreme Docs Wrestler");
    campaignRepository
        .findActiveByWrestler(player)
        .ifPresent(c -> campaignService.abandonCampaign(c));

    // 2. Navigate to the campaign page — with no active campaign the picker should appear
    navigateTo("campaign");
    waitForVaadinClientToLoad();

    captureCaption(
        "When your wrestler has no active campaign, the Campaign page invites you to start"
            + " one. With the ATW Extreme expansion enabled, multiple campaign paths are"
            + " available — each with its own storyline and difficulty.",
        4500);

    // 3. Click Start Campaign to open the chapter picker
    try {
      WebElement startBtn =
          waitForVaadinElement(driver, By.cssSelector("[id='start-campaign-btn']"));
      clickElement(startBtn);
      waitForVaadinClientToLoad();
      sleep(800);

      captureCaption(
          "The chapter picker lists every campaign path your wrestler is eligible for."
              + " Extreme legends (Rob Van Dam, Sabu, Raven, Daemon) also see The Extreme"
              + " Path — a HARD difficulty campaign with no rules and no limits.",
          5000);

      // 4. Show the outsider path selected
      try {
        WebElement outsiderOption = driver.findElement(By.xpath("//*[contains(., \"Outsider\")]"));
        clickElement(outsiderOption);
        sleep(600);
        captureCaption(
            "The Outsider's Extreme Path is open to any wrestler. You'll face the extreme"
                + " icons on their own turf — adapting your style to survive the underground"
                + " and claim the ATW Extreme Championship.",
            5000);
      } catch (Exception e) {
        log.warn("Outsider option not clickable during video recording: {}", e.getMessage());
      }
    } catch (Exception e) {
      log.warn("Start campaign button not found during video recording: {}", e.getMessage());
      // Fallback: show the dashboard with a seeded outsider campaign
      campaignService.startCampaign(player);
      navigateTo("campaign");
      waitForVaadinClientToLoad();
      captureCaption(
          "Once a chapter is selected and confirmed, the Campaign Dashboard opens immediately"
              + " — showing your chapter title, entry storyline, alignment track, and"
              + " all available actions.",
          5000);
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
}

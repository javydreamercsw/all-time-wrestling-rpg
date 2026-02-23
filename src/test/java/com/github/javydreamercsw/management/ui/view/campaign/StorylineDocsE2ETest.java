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
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestone;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestoneRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.LocalDateTime;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StorylineDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStorylineRepository storylineRepository;
  @Autowired private StorylineMilestoneRepository milestoneRepository;
  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureStoryJournalView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    Campaign campaign = createCampaign(player);

    // Create a mock AI storyline
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .campaign(campaign)
            .title("The Phantom Rival")
            .description("A mysterious figure haunts your footsteps.")
            .status(CampaignStoryline.StorylineStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();
    storyline = storylineRepository.save(storyline);

    StorylineMilestone milestone =
        StorylineMilestone.builder()
            .storyline(storyline)
            .title("Shadows in the Hall")
            .description("Spot the figure backstage.")
            .narrativeGoal("Create mystery.")
            .order(0)
            .status(StorylineMilestone.MilestoneStatus.ACTIVE)
            .build();
    milestoneRepository.save(milestone);

    storyline.setCurrentMilestone(milestone);
    storylineRepository.save(storyline);

    campaign.getState().setActiveStoryline(storyline);
    campaign.getState().setCurrentChapterId(storyline.getTitle());
    campaignRepository.save(campaign);

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    // Find the journal details component and expand it
    org.openqa.selenium.WebElement journal =
        waitForVaadinElement(driver, org.openqa.selenium.By.id("story-journal-details"));
    clickElement(journal);

    waitForText("The Phantom Rival");
    waitForVaadinElement(
        driver, org.openqa.selenium.By.id("download-json-button-" + storyline.getId()));

    documentFeature(
        "Campaign",
        "Story Journal",
        "Your AI-generated adventures are preserved in the Story Journal. Here you can review"
            + " past storyline arcs and download them as JSON chapter files to share or preserve.",
        "campaign-story-journal");
  }

  private Wrestler getOrCreateWrestler(@NonNull Account account) {
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    if (!wrestlers.isEmpty()) {
      return wrestlers.get(0);
    }

    Wrestler w =
        Wrestler.builder()
            .name("Story Docs Wrestler")
            .startingHealth(100)
            .startingStamina(100)
            .account(account)
            .isPlayer(true)
            .active(true)
            .gender(Gender.MALE)
            .build();
    return wrestlerRepository.saveAndFlush(w);
  }

  private Campaign createCampaign(@NonNull Wrestler player) {
    if (campaignService.hasActiveCampaign(player)) {
      return campaignRepository.findActiveByWrestler(player).get();
    }
    return campaignService.startCampaign(player);
  }

  private void waitForText(@NonNull String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

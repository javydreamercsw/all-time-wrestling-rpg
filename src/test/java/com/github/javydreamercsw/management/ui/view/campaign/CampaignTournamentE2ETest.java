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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CampaignTournamentE2ETest extends AbstractE2ETest {

  @BeforeEach
  public void setupTournamentData() {
    // Populate Reference Data
    if (showTypeRepository.count() == 0) {
      ShowType st = new ShowType();
      st.setName("Weekly");
      st.setDescription("Weekly Show");
      st.setExpectedMatches(3);
      st.setExpectedPromos(2);
      showTypeRepository.saveAndFlush(st);
    }

    // Create Tournament User
    Account account = createTestAccount("tournamentuser", RoleName.PLAYER);
    Wrestler playerWrestler = createTestWrestler("Tournament Player");
    playerWrestler.setAccount(account);
    wrestlerRepository.saveAndFlush(playerWrestler);

    // Create Campaign
    Campaign campaign =
        Campaign.builder()
            .wrestler(playerWrestler)
            .status(com.github.javydreamercsw.management.domain.campaign.CampaignStatus.ACTIVE)
            .startedAt(java.time.LocalDateTime.now())
            .universe(defaultUniverse)
            .build();
    campaign = campaignRepository.saveAndFlush(campaign);

    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .victoryPoints(9)
            .currentGameDate(java.time.LocalDate.now())
            .build();
    campaign.setState(state);
    campaignRepository.saveAndFlush(campaign);

    // Create opponents (5 opponents -> 6 total -> Bracket size 8)
    for (int i = 1; i < 5 + 1; i++) {
      wrestlerRepository.save(
          Wrestler.builder()
              .name("Opponent " + i)
              .startingHealth(100)
              .startingStamina(100)
              .build());
    }

    // Chapter 1: The Tournament (Using Chapter 4 from All or Nothing)
    campaignChapterService.loadChapters();
    campaignService.advanceChapter(campaign); // Advance to Chapter 4
    campaignService.advanceChapter(campaign);
    campaignService.advanceChapter(campaign);
  }

  @Test
  void testTournamentFullFlow() {
    // Login as tournament user
    logout();
    login("tournamentuser", "admin123");

    // Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForAppToBeReady();

    // Verify Initial State: Round 1
    int expectedRounds = 4;

    for (int round = 1; round < expectedRounds + 1; round++) {
      // Determine expected title
      String expectedTitle = "Round " + round;
      if (round == expectedRounds) {
        expectedTitle = "Finals";
      } else if (round == expectedRounds - 1) {
        expectedTitle = "Semi-Finals";
      } else if (round == expectedRounds - 2) {
        expectedTitle = "Quarter-Finals";
      }

      // Wait for the bracket to update
      new WebDriverWait(driver, Duration.ofSeconds(30))
          .until(
              ExpectedConditions.textToBePresentInElementLocated(
                  By.tagName("h4"), "Tournament Bracket"));

      if (round < expectedRounds) {
        // Simulate Winning the match
        WebElement winButton =
            waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Sim. Win (Face)']"));
        clickElement(winButton);

        // Verify "Advance to Next Day" button
        WebElement advanceButton =
            waitForVaadinElement(
                driver, By.xpath("//vaadin-button[text()='Match Complete - Advance to Next Day']"));

        // Click Advance (Clears match, advances day, refreshes UI)
        clickElement(advanceButton);
      } else {
        // Final Round (Champion)
        WebElement winButton =
            waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Sim. Win (Face)']"));
        clickElement(winButton);

        // Verify Tournament Champion message
        new WebDriverWait(driver, Duration.ofSeconds(30))
            .until(
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//span[contains(text(), '🏆 You are the Tournament Champion!')]")));

        // Verify Chapter Completion Button
        waitForVaadinElement(
            driver, By.xpath("//vaadin-button[text()='Complete Chapter & Advance']"));
      }
    }
  }
}

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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
public class HallOfFameDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureHallOfFame() {
    login("admin", "admin123");

    // Navigate to Hall of Fame
    navigateTo("hall-of-fame");

    // Wait for grid to load
    waitForText("Hall of Fame");
    waitForVaadinElement(driver, By.id("hall-of-fame-grid"));

    documentFeature(
        "Dashboards",
        "Hall of Fame",
        """
        The Hall of Fame tracks the top players across the entire ATW RPG ecosystem.\
         Ranking is primarily based on the Legacy Score, which combines cumulative fans\
         earned across all managed wrestlers and points from unlocked achievements.\
        """,
        "hall-of-fame");
  }

  @Test
  void testCapturePlayerLegacyDashboard() {
    // Setup: Assign a wrestler to the player account so dashboard content is visible
    Account playerAcc = accountRepository.findByUsername("player").orElseThrow();
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    if (!wrestlers.isEmpty()) {
      Wrestler w = wrestlers.get(0);
      w.setAccount(playerAcc);
      wrestlerRepository.save(w);
    }

    login("player", "player123");

    // Navigate to Player Dashboard
    navigateTo("player");

    // Wait for profile card legacy info
    waitForText("Legacy:");
    waitForText("Prestige:");

    documentFeature(
        "Player Dashboard",
        "Career Legacy",
        """
        Your persistent career progress is tracked at the account level. View your current Legacy\
         Score, Prestige XP, and earned achievement badges directly from your dashboard.\
         These stats persist across seasons and different wrestlers.\
        """,
        "player-career-legacy");

    // Navigate to Achievements tab
    click("vaadin-tab", "Achievements");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("achievements-grid"));

    documentFeature(
        "Player Dashboard",
        "Medals & Achievements",
        """
        Track your progress toward various career milestones in the Achievements tab. Each\
         achievement awards permanent Prestige XP and a unique medal displayed on your\
         profile.\
        """,
        "player-achievements");
  }

  @Test
  void testRecordHallOfFameWalkthrough() {
    setVideoInfo("Dashboards", "Hall of Fame Walkthrough", "hall-of-fame-walkthrough");

    login("admin", "admin123");
    navigateTo("hall-of-fame");
    waitForVaadinElement(driver, By.id("hall-of-fame-grid"));
    waitForVaadinClientToLoad();

    captureCaption(
        "Hall of Fame — ranks every player in the promotion by Legacy Score. The score"
            + " accumulates across all seasons and wrestlers you've managed: fan growth,"
            + " championship reigns, and achievement unlocks all contribute.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(800);

    captureCaption(
        "Prestige is the XP layer beneath Legacy Score — it increases when you unlock"
            + " achievements or reach milestone fan counts. High Prestige unlocks new"
            + " character customisation options and backstage perk slots.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(600);

    captureCaption(
        "The Hall of Fame is global — every account in the system appears here. Use it"
            + " to benchmark your career progress against other promoters and set long-term"
            + " goals for the season.",
        4000);

    sleep(1500);
  }

  private void waitForText(final String text) {
    waitForVaadinElement(driver, org.openqa.selenium.By.xpath("//*[contains(., '" + text + "')]"));
  }
}

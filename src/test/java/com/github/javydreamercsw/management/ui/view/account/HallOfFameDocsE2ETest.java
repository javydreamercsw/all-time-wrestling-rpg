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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

public class HallOfFameDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureHallOfFame() {
    login("admin", "admin123");

    // Navigate to Hall of Fame
    driver.get("http://localhost:" + serverPort + getContextPath() + "/hall-of-fame");
    waitForVaadinClientToLoad();

    // Wait for grid to load
    waitForText("Hall of Fame");
    waitForVaadinElement(driver, By.id("hall-of-fame-grid"));

    documentFeature(
        "Dashboards",
        "Hall of Fame",
        "The Hall of Fame tracks the top players across the entire ATW RPG ecosystem."
            + " Ranking is primarily based on the Legacy Score, which combines cumulative fans"
            + " earned across all managed wrestlers and points from unlocked achievements.",
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
    driver.get("http://localhost:" + serverPort + getContextPath() + "/player");
    waitForVaadinClientToLoad();

    // Wait for profile card legacy info
    waitForText("Legacy:");
    waitForText("Prestige:");

    documentFeature(
        "Player Dashboard",
        "Career Legacy",
        "Your persistent career progress is tracked at the account level. View your current Legacy"
            + " Score, Prestige XP, and earned achievement badges directly from your dashboard."
            + " These stats persist across seasons and different wrestlers.",
        "player-career-legacy");

    // Navigate to Achievements tab
    click("vaadin-tab", "Achievements");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("achievements-grid"));

    documentFeature(
        "Player Dashboard",
        "Medals & Achievements",
        "Track your progress toward various career milestones in the Achievements tab. Each"
            + " achievement awards permanent Prestige XP and a unique medal displayed on your"
            + " profile.",
        "player-achievements");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

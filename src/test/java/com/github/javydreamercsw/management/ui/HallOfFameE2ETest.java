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
package com.github.javydreamercsw.management.ui;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HallOfFameE2ETest extends AbstractE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testHallOfFameVisibility() {
    login("admin", "admin123");

    // Navigate to Hall of Fame
    driver.get("http://localhost:" + serverPort + getContextPath() + "/hall-of-fame");

    // Wait for grid to load
    waitForPageSourceToContain("Hall of Fame");

    // Verify grid exists and has headers
    WebElement grid = driver.findElement(By.id("hall-of-fame-grid"));
    Assertions.assertNotNull(grid);

    // Verify some players are listed (at least the default ones from DataInitializer)
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            d -> {
              List<WebElement> cells =
                  d.findElements(By.cssSelector("#hall-of-fame-grid vaadin-grid-cell-content"));
              return cells.size() > 0;
            });

    String pageSource = driver.getPageSource();
    Assertions.assertTrue(pageSource.contains("admin"));
    Assertions.assertTrue(pageSource.contains("booker"));
    Assertions.assertTrue(pageSource.contains("player"));
  }

  @Test
  void testPlayerLegacyDashboard() {
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

    // Wait for profile card
    waitForPageSourceToContain("Legacy:");
    waitForPageSourceToContain("Prestige:");

    String pageSource = driver.getPageSource();
    Assertions.assertTrue(pageSource.contains("Legacy:"));
    Assertions.assertTrue(pageSource.contains("Prestige:"));
  }
}

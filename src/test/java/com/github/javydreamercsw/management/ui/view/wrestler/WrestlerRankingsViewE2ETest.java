/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

public class WrestlerRankingsViewE2ETest extends AbstractE2ETest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Autowired private TitleService titleService;

  @BeforeEach
  public void setupChampion() {
    // Check via API if a champion is assigned
    if (!isChampionAssigned()) {
      assignChampionViaApi();
    }
  }

  @Test
  public void testChampionIcon() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to be present
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(grid);

    WebElement trophyIcon =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-icon[icon*='trophy']")));
    assertNotNull(trophyIcon);
    String iconAttr = trophyIcon.getAttribute("icon");
    assertNotNull(iconAttr);
    assertTrue(iconAttr.contains("trophy"));
  }

  // Use the API to check if a champion is assigned
  private boolean isChampionAssigned() {
    return titleRepository.findAll().stream()
        .anyMatch(title -> title.getChampion() != null && !title.getChampion().isEmpty());
  }

  // Use the API to assign a champion if not present
  private void assignChampionViaApi() {
    List<Title> titles = titleRepository.findAll();
    if (!titles.isEmpty()) {
      Title title = titles.get(0);
      List<Wrestler> eligible = titleService.getEligibleChallengers(title.getId());
      if (!eligible.isEmpty()) {
        titleService.awardTitleTo(
            title,
            Arrays.asList(
                wrestlerRepository
                    .findAll()
                    .get(new Random().nextInt(wrestlerRepository.findAll().size()))));
      }
    }
  }
}

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
package com.github.javydreamercsw.management.ui;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import java.time.Duration;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class GenderFilteringE2ETest extends AbstractE2ETest {

  @Autowired private TierRecalculationService tierRecalculationService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private SegmentRepository segmentRepository;

  private Wrestler maleWrestler;
  private Wrestler femaleWrestler;
  private Title womensTitle;

  @BeforeEach
  @Transactional
  public void setupTestData() {
    segmentRepository.deleteAll();
    wrestlerRepository.deleteAll();
    titleRepository.deleteAll();

    maleWrestler = new Wrestler();
    maleWrestler.setName("Male Wrestler");
    maleWrestler.setFans(100000L);
    maleWrestler.setGender(Gender.MALE);
    maleWrestler.setTier(WrestlerTier.MIDCARDER);
    maleWrestler.setDeckSize(15);
    maleWrestler.setStartingHealth(15);
    maleWrestler.setLowHealth(0);
    maleWrestler.setStartingStamina(0);
    maleWrestler.setLowStamina(0);
    maleWrestler.setIsPlayer(false);
    maleWrestler.setBumps(0);
    wrestlerRepository.save(maleWrestler);

    femaleWrestler = new Wrestler();
    femaleWrestler.setName("Female Wrestler");
    femaleWrestler.setFans(120000L);
    femaleWrestler.setGender(Gender.FEMALE);
    femaleWrestler.setTier(WrestlerTier.MIDCARDER);
    femaleWrestler.setDeckSize(15);
    femaleWrestler.setStartingHealth(15);
    femaleWrestler.setLowHealth(0);
    femaleWrestler.setStartingStamina(0);
    femaleWrestler.setLowStamina(0);
    femaleWrestler.setIsPlayer(false);
    femaleWrestler.setBumps(0);
    wrestlerRepository.save(femaleWrestler);

    womensTitle = new Title();
    womensTitle.setName("Women's World Championship");
    womensTitle.setGender(Gender.FEMALE);
    womensTitle.setTier(WrestlerTier.MIDCARDER);
    titleRepository.save(womensTitle);

    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlerRepository.findAll()));
  }

  @Test
  public void testFemaleGenderFiltering() {
    try {
      // Navigate to the Wrestler Rankings view
      log.info("Navigating to wrestler rankings");
      driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");

      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

      // Verify both wrestlers are displayed initially
      log.info("Verifying both wrestlers are displayed");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), maleWrestler.getName()));
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), femaleWrestler.getName()));

      // Select "FEMALE" from the gender ComboBox
      log.info("Filtering by FEMALE");
      WebElement genderComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("gender-selection")));
      Assertions.assertNotNull(genderComboBox);
      selectFromVaadinComboBox(genderComboBox, "FEMALE");

      // Verify only the female wrestler is displayed
      log.info("Verifying only female wrestler is displayed");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), femaleWrestler.getName()));
      wait.until(
          ExpectedConditions.invisibilityOfElementWithText(
              By.tagName("vaadin-grid-cell-content"), maleWrestler.getName()));
    } catch (Exception e) {
      log.error("Error during E2E test", e);
      Assertions.fail(e);
    }
  }

  @Test
  public void testMaleGenderFiltering() {
    try {
      // Navigate to the Wrestler Rankings view
      log.info("Navigating to wrestler rankings");
      driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");

      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

      // Verify both wrestlers are displayed initially
      log.info("Verifying both wrestlers are displayed");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), maleWrestler.getName()));
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), femaleWrestler.getName()));

      // Select "MALE" from the gender ComboBox
      log.info("Filtering by MALE");
      WebElement genderComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("gender-selection")));
      Assertions.assertNotNull(genderComboBox);
      selectFromVaadinComboBox(genderComboBox, "MALE");

      // Verify only the male wrestler is displayed
      log.info("Verifying only male wrestler is displayed");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), maleWrestler.getName()));
      wait.until(
          ExpectedConditions.invisibilityOfElementWithText(
              By.tagName("vaadin-grid-cell-content"), femaleWrestler.getName()));
    } catch (Exception e) {
      log.error("Error during E2E test", e);
      Assertions.fail(e);
    }
  }

  @Test
  public void testChampionshipAndTierBoundaries() {
    try {
      // Navigate to the Championship Rankings view
      log.info("Navigating to championship rankings");
      driver.get("http://localhost:" + serverPort + getContextPath() + "/championship-rankings");

      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

      // Select the women's championship
      log.info("Selecting women's championship");
      WebElement championshipComboBox =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.cssSelector("vaadin-combo-box")));
      Assertions.assertNotNull(championshipComboBox);
      championshipComboBox.sendKeys(womensTitle.getName(), Keys.TAB);

      // Verify the female wrestler is in the contenders list
      log.info("Verifying female wrestler is a contender");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.tagName("vaadin-grid"), femaleWrestler.getName()));

      // Open the "Tier Boundaries" dialog
      log.info("Opening tier boundaries dialog");
      WebElement showTierBoundariesButton =
          wait.until(ExpectedConditions.elementToBeClickable(By.id("show-tier-boundaries-button")));
      Assertions.assertNotNull(showTierBoundariesButton);
      clickElement(showTierBoundariesButton);

      // Wait for the dialog to appear
      log.info("Waiting for dialog");
      wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

      // Select "FEMALE" in the dialog's gender ComboBox
      log.info("Filtering tier boundaries by FEMALE");
      WebElement dialogGenderComboBox =
          driver.findElement(By.cssSelector("vaadin-dialog-overlay vaadin-combo-box"));
      selectFromVaadinComboBox(dialogGenderComboBox, "FEMALE");

      // Verify that the female tier boundaries are displayed in the dialog's grid
      log.info("Verifying female tier boundaries");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.cssSelector("vaadin-dialog-overlay vaadin-grid"), "Midcarder"));
    } catch (Exception e) {
      log.error("Error during E2E test", e);
      Assertions.fail(e);
    }
  }
}

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
package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

class TitleListDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TitleService titleService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Tag("video")
  @Test
  void testRecordTitleListWalkthrough() {
    setVideoInfo("Entities", "Championship Titles Walkthrough", "title-list-walkthrough");

    // Ensure there is at least one title to show
    if (titleRepository.count() == 0) {
      Title t = new Title();
      t.setName("World Heavyweight Title");
      t.setTier(WrestlerTier.MAIN_EVENTER);
      t.setChampionshipType(ChampionshipType.SINGLE);
      t.setIsActive(true);
      titleService.save(t);
    }

    navigateTo("title-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    captureCaption(
        "Title List — every championship belt in the promotion is managed here."
            + " Each row shows the title's tier, championship type, current champion,"
            + " and eligible challengers. The Challengers ComboBox lets bookers assign"
            + " contenders for upcoming title matches inline.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    sleep(1200);
    captureCaption(
        "Challengers are filtered automatically by tier and gender to match the title's"
            + " eligibility rules. A Rookie-tier title only shows Rookie-tier wrestlers"
            + " in the contender list — keeping championship booking logical.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);

    // Click the Create Title button to show the dialog
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement createButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Create Title')]")));
    captureCaption(
        "Click Create Title to define a new championship. You can configure its tier,"
            + " gender restriction, championship type (Single, Tag Team, Trios), and"
            + " optionally assign an image.",
        4000);
    clickElement(createButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    captureCaption(
        "The Create Title dialog — Name and Tier are required. Tier controls which"
            + " wrestlers are eligible to compete for it, and Championship Type determines"
            + " whether it is a singles, tag team, or trios title.",
        4500);

    // Close dialog without saving — we just wanted to show it
    WebElement cancelButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//vaadin-button[text()='Cancel']")));
    clickElement(cancelButton);

    sleep(1500);
  }

  @Test
  void testCaptureTitleListScreenshot() {
    // Ensure there are titles to show
    if (titleRepository.count() == 0) {
      Title t = new Title();
      t.setName("World Heavyweight Title");
      t.setTier(WrestlerTier.MAIN_EVENTER);
      t.setChampionshipType(ChampionshipType.SINGLE);
      t.setIsActive(true);
      titleService.save(t);
    }

    navigateTo("title-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    documentFeature(
        "Entities",
        "Championship Titles",
        "Manage every championship belt in the promotion. Each title has a configurable"
            + " tier (controls contender eligibility), championship type, and gender"
            + " restriction. The Challengers column lets bookers assign contenders inline"
            + " without leaving the list view.",
        "title-list-overview");
  }

  @Test
  void testCaptureTitleCreateDialogScreenshot() {
    navigateTo("title-list");
    waitForVaadinClientToLoad();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement createButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Create Title')]")));
    clickElement(createButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(600);

    documentFeature(
        "Entities",
        "Create Championship Title",
        "Define a new championship: enter the name, select the tier and championship type,"
            + " optionally restrict by gender, and upload a belt image. Once saved the"
            + " title appears in the list and becomes available for match booking.",
        "title-create-dialog");
  }
}

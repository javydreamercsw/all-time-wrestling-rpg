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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WrestlerProfileViewE2ETest extends AbstractE2ETest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private NpcService npcService;

  private Wrestler testWrestler;

  @BeforeEach
  void setUp() {
    // Clear all relevant repositories to ensure a clean state for each test
    cleanupLeagues();
    titleReignRepository
        .findAll()
        .forEach(
            reign -> {
              reign.setWonAtSegment(null);
              titleReignRepository.save(reign);
            });
    titleReignRepository.deleteAll();
    titleRepository
        .findAll()
        .forEach(
            title -> {
              title.setChampion(null);
              titleRepository.save(title);
            });
    multiWrestlerFeudRepository.deleteAll();
    segmentRepository.deleteAll();
    showRepository.deleteAll();
    wrestlerRepository.deleteAll();
    seasonRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();

    testWrestler = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Test Wrestler"));
    // Ensure a default season exists for tests
    if (seasonService.findByName("Default Season") == null) {
      seasonService.createSeason("Default Season", "Default Season", 4);
    }
    if (showTypeRepository.findByName("Weekly").isEmpty()) {
      ShowType showType = new ShowType();
      showType.setName("Weekly");
      showType.setDescription("A weekly show");
      showTypeRepository.saveAndFlush(showType);
    }
    if (segmentTypeService.findByName("One on One").isEmpty()) {
      segmentTypeService.createOrUpdateSegmentType("One on One", "1 vs 1 match");
    }
    if (segmentRuleRepository.findByName("Normal").isEmpty()) {
      SegmentRule rule = new SegmentRule();
      rule.setName("Normal");
      rule.setDescription("Normal match rules.");
      segmentRuleRepository.save(rule);
    }
  }

  @Test
  void testWrestlerProfileLoads() {
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + testWrestler.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-name")));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("wrestler-name"), testWrestler.getName()));

    WebElement wrestlerName = driver.findElement(By.id("wrestler-name"));
    assertEquals(testWrestler.getName(), wrestlerName.getText());

    WebElement wrestlerDetails = driver.findElement(By.tagName("p"));
    assertTrue(wrestlerDetails.getText().contains("Gender: " + testWrestler.getGender()));
    assertTrue(wrestlerDetails.getText().contains("Fans: " + testWrestler.getFans()));

    // Verify that manager is not displayed
    assertTrue(driver.findElements(By.id("manager-name")).isEmpty());
  }

  @Test
  void testFeudHistorySorting() {
    // Given
    Wrestler wrestler1 = TestUtils.createWrestler("Wrestler 1");
    wrestler1.setGender(Gender.MALE);
    wrestler1.setFans(1000L);
    wrestlerRepository.saveAndFlush(wrestler1);

    Wrestler wrestler2 = TestUtils.createWrestler("Wrestler 2");
    wrestler2.setGender(Gender.MALE);
    wrestler2.setFans(1000L);
    wrestlerRepository.saveAndFlush(wrestler2);

    Assertions.assertNotNull(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    Rivalry testRivalry =
        rivalryService.createRivalry(wrestler1.getId(), wrestler2.getId(), "Test Rivalry").get();
    rivalryService.addHeat(testRivalry.getId(), 100, "Initial Rivalry Heat");

    MultiWrestlerFeud feud =
        multiWrestlerFeudService
            .createFeud("Test Feud", "Test Feud Description", "Test Feud Storyline")
            .get();
    Assertions.assertNotNull(feud.getId());
    multiWrestlerFeudService.addHeat(feud.getId(), 200, "Initial Heat");
    multiWrestlerFeudService.addParticipant(feud.getId(), wrestler1.getId(), FeudRole.PROTAGONIST);

    // When
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler1.getId());

    // Then
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[text()='Feud History']")));

    WebElement feudHistory = driver.findElement(By.xpath("//h3[text()='Feud History']"));
    WebElement feudParagraph = feudHistory.findElement(By.xpath("following-sibling::p[1]"));
    wait.until(
        ExpectedConditions.textToBePresentInElement(feudParagraph, "Feud: Test Feud (Heat: 200)"));
    WebElement rivalryParagraph = feudHistory.findElement(By.xpath("following-sibling::p[2]"));

    assertTrue(feudParagraph.getText().contains("Feud: Test Feud (Heat: 200)"));
    assertTrue(rivalryParagraph.getText().contains("Rivalry with Wrestler 2 (Heat: 100)"));
  }

  @Test
  void testRecentMatchesGrid() {
    // Given
    Wrestler wrestler1 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 1"));
    wrestler1.setGender(Gender.MALE);
    wrestler1.setFans(1000L);
    wrestlerRepository.saveAndFlush(wrestler1);

    Title title =
        titleService.createTitle(
            "Test Title", "Test Title", WrestlerTier.ROOKIE, ChampionshipType.SINGLE);

    Season season = seasonService.createSeason("Test Season", "Test Season", 5);
    Show show =
        showService.createShow(
            "Test Show",
            "Test Show",
            showTypeRepository.findByName("Weekly").get().getId(),
            null,
            season.getId(),
            null,
            null,
            null,
            null);

    SegmentType matchType = segmentTypeService.findByName("One on One").get();
    SegmentRule rule = segmentRuleService.findByName("Normal").get();

    Segment segment = segmentService.createSegment(show, matchType, Instant.now());
    segment.addParticipant(wrestler1);
    segment.setWinners(List.of(wrestler1));
    segment.getTitles().add(title);
    segment.getSegmentRules().add(rule);
    segmentService.updateSegment(segment);

    // When
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler1.getId());

    // Then
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath("//vaadin-grid-cell-content[text()='Wrestler 1']"), "Wrestler 1"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath("//vaadin-grid-cell-content[text()='Wrestler 1']"), "Wrestler 1"));
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath("//vaadin-grid-cell-content[text()='Test Title']"), "Test Title"));
  }

  @Test
  void testManagerIsDisplayed() {
    // Given
    Npc manager = new Npc();
    manager.setName("Test Manager");
    manager.setNpcType("Manager");
    manager = npcService.save(manager);

    Wrestler wrestlerWithManager = TestUtils.createWrestler("Managed Wrestler");
    wrestlerWithManager.setManager(manager);
    wrestlerWithManager = wrestlerRepository.saveAndFlush(wrestlerWithManager);

    // When
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestlerWithManager.getId());

    // Then
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement managerName =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("manager-name")));
    assertEquals("Managed by: Test Manager", managerName.getText());
  }

  @Test
  void testTitleHistoryIsVisible() {
    // Given
    Wrestler wrestler1 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Champion Wrestler"));

    Title title =
        titleService.createTitle(
            "World Title", "The top title", WrestlerTier.MAIN_EVENTER, ChampionshipType.SINGLE);

    Season season = seasonService.createSeason("History Season", "Season for history", 10);
    Show show =
        showService.createShow(
            "SuperShow",
            "Big Event",
            showTypeRepository.findByName("Weekly").get().getId(),
            null,
            season.getId(),
            null,
            null,
            null,
            null);

    SegmentType matchType = segmentTypeService.findByName("One on One").get();
    Segment segment =
        segmentService.createSegment(show, matchType, Instant.now().minusSeconds(1000));
    segment.addParticipant(wrestler1);
    segment.setWinners(List.of(wrestler1));
    segment = segmentRepository.saveAndFlush(segment);

    // Award title at the segment
    title.awardTitleTo(List.of(wrestler1), Instant.now().minusSeconds(500), segment);
    titleRepository.saveAndFlush(title);

    // When
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler1.getId());

    // Then
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[text()='Title History']")));

    // Verify timeline presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='CURRENT']")));

    // Verify card presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='World Title']")));

    // Verify match link
    WebElement link =
        waitForVaadinElement(driver, By.xpath("//a[contains(text(), 'Won at: SuperShow')]"));
    assertNotNull(link);

    // Click and verify navigation
    clickElement(link);
    wait.until(ExpectedConditions.urlContains("show-detail/" + show.getId()));
  }
}

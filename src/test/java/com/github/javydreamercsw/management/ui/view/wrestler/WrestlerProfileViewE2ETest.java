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
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Tag("video")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WrestlerProfileViewE2ETest extends AbstractE2ETest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private NpcService npcService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;

  private Wrestler testWrestler;

  @BeforeEach
  public void setUp() {
    // Clear all relevant repositories to ensure a clean state for each test
    cleanupLeagues();

    testWrestler = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Test Wrestler"));
    wrestlerService.getOrCreateState(testWrestler.getId(), defaultUniverse.getId());

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

    WrestlerState state =
        wrestlerService.getOrCreateState(testWrestler.getId(), defaultUniverse.getId());
    assertTrue(wrestlerDetails.getText().contains("Fans: " + state.getFans()));

    // Verify that manager is not displayed
    assertTrue(driver.findElements(By.id("manager-name")).isEmpty());
  }

  @Test
  void testFeudHistorySorting() {
    setVideoInfo("Wrestler Profile", "Feud and Rivalry History", "wrestler-feud-history");
    // Given
    Wrestler wrestler1 = TestUtils.createWrestler("Wrestler 1");
    wrestler1.setGender(Gender.MALE);
    wrestler1 = wrestlerRepository.saveAndFlush(wrestler1);
    WrestlerState state1 =
        wrestlerService.getOrCreateState(wrestler1.getId(), defaultUniverse.getId());
    state1.setFans(1000L);
    wrestlerStateRepository.saveAndFlush(state1);

    Wrestler wrestler2 = TestUtils.createWrestler("Wrestler 2");
    wrestler2.setGender(Gender.MALE);
    wrestler2 = wrestlerRepository.saveAndFlush(wrestler2);
    WrestlerState state2 =
        wrestlerService.getOrCreateState(wrestler2.getId(), defaultUniverse.getId());
    state2.setFans(1000L);
    wrestlerStateRepository.saveAndFlush(state2);

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
    expandAccordionPanel("Rivalry History");
    captureCaption(
        "Wrestler Profile — the Rivalry History accordion shows every active feud and"
            + " rivalry, sorted by heat so the hottest storylines appear first. Heat is"
            + " accumulated via match outcomes, promos, and drama events.",
        4500);

    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[text()='Feud History']")));

    WebElement feudHistory = driver.findElement(By.xpath("//h3[text()='Feud History']"));
    WebElement feudParagraph = feudHistory.findElement(By.xpath("following-sibling::p[1]"));
    wait.until(
        ExpectedConditions.textToBePresentInElement(feudParagraph, "Feud: Test Feud (Heat: 200)"));
    WebElement rivalryParagraph = feudHistory.findElement(By.xpath("following-sibling::p[2]"));

    assertTrue(feudParagraph.getText().contains("Feud: Test Feud (Heat: 200)"));
    assertTrue(rivalryParagraph.getText().contains("Rivalry with Wrestler 2 (Heat: 100)"));
    captureCaption(
        "Feuds and rivalries are ranked by heat — high-heat storylines rise to the top,"
            + " helping bookers identify which angles to push on upcoming shows. Each entry"
            + " shows the current heat total accumulated across all events in that feud.",
        4500);
  }

  @Test
  void testRecentMatchesGrid() {
    setVideoInfo("Wrestler Profile", "Recent Match Log", "wrestler-recent-matches");
    // Given
    Wrestler wrestler1 = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler 1"));
    wrestler1.setGender(Gender.MALE);
    wrestler1 = wrestlerRepository.saveAndFlush(wrestler1);
    WrestlerState state1 =
        wrestlerService.getOrCreateState(wrestler1.getId(), defaultUniverse.getId());
    state1.setFans(1000L);
    wrestlerStateRepository.saveAndFlush(state1);

    Title title =
        titleService.createTitle(
            "Test Title",
            "Test Title",
            WrestlerTier.ROOKIE,
            ChampionshipType.SINGLE,
            defaultUniverse.getId());

    Season season = seasonService.createSeason("Test Season", "Test Season", 5);
    Show show =
        showService.createShow(
            "Test Show",
            "Test Show",
            showTypeRepository.findByName("Weekly").get().getId(),
            null,
            season.getId(),
            null,
            defaultUniverse.getId(),
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
    expandAccordionPanel("Match Logs");
    captureCaption(
        "The Match Logs accordion shows every match the wrestler has competed in —"
            + " opponents, titles on the line, outcome, and the show it appeared on."
            + " Click the show name to navigate directly to that show's detail view.",
        4500);
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
    captureCaption(
        "Championship matches are highlighted — the title at stake is shown alongside"
            + " the participants, giving a full picture of the wrestler's title history."
            + " This data feeds into the Championships accordion on the same profile.",
        4000);
  }

  @Test
  void testManagerIsDisplayed() {
    // Given
    Npc manager = new Npc();
    manager.setName("Test Manager");
    manager.setNpcType("Manager");
    manager = npcService.save(manager);

    Wrestler wrestlerWithManager =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Managed Wrestler"));
    WrestlerState managerState =
        wrestlerService.getOrCreateState(wrestlerWithManager.getId(), defaultUniverse.getId());
    managerState.setManager(manager);
    wrestlerStateRepository.save(managerState);

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
    setVideoInfo("Wrestler Profile", "Championship Title History", "wrestler-title-history");
    // Given
    Wrestler wrestler1 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Champion Wrestler"));

    Title title =
        titleService.createTitle(
            "World Title",
            "The top title",
            WrestlerTier.MAIN_EVENTER,
            ChampionshipType.SINGLE,
            defaultUniverse.getId());

    Season season = seasonService.createSeason("History Season", "Season for history", 10);
    Show show =
        showService.createShow(
            "SuperShow",
            "Big Event",
            showTypeRepository.findByName("Weekly").get().getId(),
            null,
            season.getId(),
            null,
            defaultUniverse.getId(),
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
    expandAccordionPanel("Championships");
    captureCaption(
        "The Championships accordion shows a wrestler's full title history as a timeline —"
            + " every reign, the shows it started on, and whether it's currently active."
            + " This is the canonical record of a wrestler's championship lineage.",
        4500);

    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[text()='Title History']")));

    // Verify timeline presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='CURRENT']")));

    // Verify card presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='World Title']")));
    captureCaption(
        "Active reigns are marked CURRENT. Each entry links directly to the show where"
            + " the title was won — click the link to navigate to that show's detail view"
            + " and review the full card from that night.",
        4000);

    // Verify match link
    WebElement link =
        waitForVaadinElement(driver, By.xpath("//a[contains(text(), 'Won at: SuperShow')]"));
    assertNotNull(link);

    // Click and verify navigation
    clickElement(link);
    wait.until(ExpectedConditions.urlContains("show-detail/" + show.getId()));
    captureCaption(
        "The show detail view opens — you can review the full card and segment results"
            + " from the night the title changed hands. All segments, participants, and"
            + " AI-generated narration are preserved in the show record.",
        4500);
  }

  private void expandAccordionPanel(final String label) {
    waitForVaadinClientToLoad();
    List<WebElement> panels = driver.findElements(By.tagName("vaadin-accordion-panel"));
    for (WebElement panel : panels) {
      if (panel.getText().contains(label)) {
        clickElement(panel);
        // Wait for animation
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
        return;
      }
    }
    throw new RuntimeException("Could not find accordion panel with label: " + label);
  }
}

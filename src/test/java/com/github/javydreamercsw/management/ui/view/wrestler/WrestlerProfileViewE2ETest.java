package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
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

class WrestlerProfileViewE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private RivalryService rivalryService;
  @Autowired private MultiWrestlerFeudService multiWrestlerFeudService;
  @Autowired private TitleService titleService;
  @Autowired private ShowService showService;
  @Autowired private SeasonService seasonService;
  @Autowired private SegmentService segmentService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private WrestlerRepository wrestlerRepository;

  private Wrestler testWrestler;

  @BeforeEach
  void setUp() {
    testWrestler = TestUtils.createWrestler(wrestlerRepository, "Test Wrestler");
  }

  @Test
  void testWrestlerProfileLoads() {
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + testWrestler.getId());

    // Wait for the view to load
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-name")));

    WebElement wrestlerName = driver.findElement(By.id("wrestler-name"));
    assertEquals(testWrestler.getName(), wrestlerName.getText());

    WebElement wrestlerDetails = driver.findElement(By.tagName("p"));
    assertTrue(wrestlerDetails.getText().contains("Gender: " + testWrestler.getGender()));
    assertTrue(wrestlerDetails.getText().contains("Fans: " + testWrestler.getFans()));
  }

  @Test
  void testFeudHistorySorting() {
    // Given
    Wrestler wrestler1 = TestUtils.createWrestler(wrestlerRepository, "Wrestler 1");
    wrestler1.setGender(Gender.MALE);
    wrestler1.setFans(1000L);
    wrestler1 = wrestlerService.save(wrestler1);

    Wrestler wrestler2 = TestUtils.createWrestler(wrestlerRepository, "Wrestler 2");
    wrestler2.setGender(Gender.MALE);
    wrestler2.setFans(1000L);
    wrestler2 = wrestlerService.save(wrestler2);

    Assertions.assertNotNull(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    rivalryService.createRivalry(wrestler1.getId(), wrestler2.getId(), "Test Rivalry");

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
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h3")));

    WebElement feudHistory = driver.findElement(By.xpath("//h3[text()='Feud History']"));
    WebElement feudParagraph = feudHistory.findElement(By.xpath("following-sibling::p[1]"));
    WebElement rivalryParagraph = feudHistory.findElement(By.xpath("following-sibling::p[2]"));

    assertTrue(feudParagraph.getText().contains("Feud: Test Feud (Heat: 200)"));
    assertTrue(rivalryParagraph.getText().contains("Rivalry with Wrestler 2 (Heat: 100)"));
  }

  @Test
  void testRecentMatchesGrid() {
    // Given
    Wrestler wrestler1 = TestUtils.createWrestler(wrestlerRepository, "Wrestler 1");
    wrestler1.setGender(Gender.MALE);
    wrestler1.setFans(1000L);
    wrestler1 = wrestlerService.save(wrestler1);

    Title title = titleService.createTitle("Test Title", "Test Title", WrestlerTier.ROOKIE);

    Season season = seasonService.createSeason("Test Season", "Test Season", 5);
    Show show = showService.createShow("Test Show", "Test Show", season.getId(), null, null, null);

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

    WebElement grid = driver.findElement(By.tagName("vaadin-grid"));
    WebElement participants =
        grid.findElement(By.xpath("//vaadin-grid-cell-content[text()='Wrestler 1']"));
    WebElement winners =
        grid.findElement(By.xpath("//vaadin-grid-cell-content[text()='Wrestler 1']"));
    WebElement championships =
        grid.findElement(By.xpath("//vaadin-grid-cell-content[text()='Test Title']"));

    assertTrue(participants.isDisplayed());
    assertTrue(winners.isDisplayed());
    assertTrue(championships.isDisplayed());
  }
}

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
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class FullShowLifecycleE2ETest extends AbstractE2ETest {
  private static final String SHOW_TYPE_NAME = "Weekly";
  private static final String SEASON_NAME = "Test Season";
  private static final String TEMPLATE_NAME = "Continuum";

  @Autowired private org.springframework.cache.CacheManager cacheManager;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  @BeforeEach
  @Transactional
  public void setupTestData() {
    // Clear the cache to ensure we get fresh data
    if (cacheManager != null) {
      org.springframework.cache.Cache wrestlersCache = cacheManager.getCache("wrestlers");
      if (wrestlersCache != null) {
        wrestlersCache.clear();
      }
    }

    titleReignRepository.deleteAll();
    titleRepository
        .findAll()
        .forEach(
            title -> {
              title.setChampion(null);
              titleRepository.save(title);
            });
    segmentRepository.deleteAll();
    showRepository.deleteAll();
    wrestlerRepository.deleteAll();
    seasonRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    rivalryRepository.deleteAll();
    multiWrestlerFeudRepository.deleteAll();
    titleRepository.deleteAll();
    segmentTypeRepository.deleteAll();

    // Add segment types
    if (segmentTypeRepository.findByName("One on One").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("One on One");
      st.setDescription("Traditional singles wrestling match between two competitors");
      segmentTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("Tag Team").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("Tag Team");
      st.setDescription("Team-based wrestling match with tag-in/tag-out mechanics");
      segmentTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("Free-for-All").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("Free-for-All");
      st.setDescription("Multi-person match where everyone fights at once");
      segmentTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("Abu Dhabi Rumble").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("Abu Dhabi Rumble");
      st.setDescription("Large-scale elimination match with timed entries");
      segmentTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("Promo").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("Promo");
      st.setDescription("Non-wrestling segment for storyline development and character work");
      segmentTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("Handicap Match").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("Handicap Match");
      st.setDescription("A match with uneven teams, where one side has a numerical disadvantage");
      segmentTypeRepository.save(st);
    }

    // Clear and insert required ShowType
    Optional<ShowType> st = showTypeRepository.findByName(SHOW_TYPE_NAME);
    if (st.isEmpty()) {
      ShowType showType = new ShowType();
      showType.setName(SHOW_TYPE_NAME);
      showType.setDescription("A weekly show");
      showType.setExpectedMatches(5);
      showType.setExpectedPromos(2);
      showTypeRepository.save(showType);
    }

    // Clear and insert required ShowTemplate
    Optional<ShowTemplate> t = showTemplateRepository.findByName(TEMPLATE_NAME);
    if (t.isEmpty()) {
      ShowTemplate template = new ShowTemplate();
      template.setName(TEMPLATE_NAME);
      template.setShowType(showTypeRepository.findByName(SHOW_TYPE_NAME).get());
      showTemplateRepository.save(template);
    }

    // Clear and insert required Season
    Optional<Season> s = seasonRepository.findByName(SEASON_NAME);
    if (s.isEmpty()) {
      Season season = new Season();
      season.setName(SEASON_NAME);
      seasonRepository.save(season);
    }

    // Create some wrestlers for the tests
    for (int i = 0; i < 10; i++) {
      wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler " + i));
    }
  }

  @Test
  public void testFullShowLifecycle() {
    try {
      // Navigate to the Show List view
      log.info("Navigating to show list");
      driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

      final String showName = "My E2E Show";

      // Click the "Create" button
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

      // Fill in the form
      log.info("Filling out new show form");
      Objects.requireNonNull(
              wait.until(
                  ExpectedConditions.visibilityOfElementLocated(
                      By.cssSelector("vaadin-text-field"))))
          .sendKeys(showName);
      List<WebElement> comboBoxes = driver.findElements(By.cssSelector("vaadin-combo-box"));
      WebElement showTypeComboBox = comboBoxes.get(0);
      WebElement seasonComboBox = comboBoxes.get(1);
      WebElement templateComboBox = comboBoxes.get(2);

      showTypeComboBox.sendKeys(SHOW_TYPE_NAME, Keys.TAB);

      wait.until(driver -> templateComboBox.isEnabled());

      seasonComboBox.sendKeys(SEASON_NAME, Keys.TAB);
      templateComboBox.sendKeys(TEMPLATE_NAME, Keys.TAB);

      driver
          .findElement(By.id("show-date"))
          .sendKeys(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));

      // Click the "Create" button
      log.info("Creating show");
      WebElement createButton = driver.findElement(By.id("create-show-button"));
      clickAndScrollIntoView(createButton);

      log.info("Waiting for show to appear in grid");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(By.tagName("vaadin-grid"), showName));

      List<Show> matchingShows = showService.findByName(showName);
      Assertions.assertEquals(1, matchingShows.size());
      Show show = matchingShows.get(0);

      // Click on the newly created show in the grid to navigate to its detail page
      log.info("Navigating to show detail page");
      WebElement viewShowDetails =
          wait.until(
              ExpectedConditions.elementToBeClickable(
                  By.id("view-details-button-" + show.getId())));
      clickAndScrollIntoView(viewShowDetails);

      // Verify navigation to the show detail view (or planning view)
      log.info("Waiting for show detail URL");
      wait.until(ExpectedConditions.urlContains("/show-detail"));

      // Click the "Planning Show" button
      log.info("Navigating to show planning view");
      WebElement planningShowButton =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("plan-show-button")));
      clickAndScrollIntoView(planningShowButton);

      // Verify navigation to the show planning view
      log.info("Waiting for show planning URL");
      wait.until(ExpectedConditions.urlContains("/show-planning"));

      log.info("Waiting for show planning context area");
      WebElement showPlanningContextArea =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("show-planning-context-area")));
      wait.until(driver -> !showPlanningContextArea.getText().isEmpty());
      Assertions.assertFalse(showPlanningContextArea.getText().contains("Error"));

      // Click the "Propose Segments" button
      log.info("Proposing segments");
      WebElement proposeSegmentsButton =
          wait.until(ExpectedConditions.elementToBeClickable(By.id("propose-segments-button")));
      clickAndScrollIntoView(proposeSegmentsButton);

      log.info("Waiting for proposed segments grid");
      wait.until(
          ExpectedConditions.presenceOfElementLocated(
              By.cssSelector("vaadin-grid#proposed-segments-grid vaadin-grid-cell-content")));

      // Approve segments
      log.info("Approving segments");
      WebElement approveButton =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("approve-segments-button")));
      clickAndScrollIntoView(approveButton);

      // Wait for the notification that segments are approved to appear and disappear
      log.info("Waiting for notification");
      wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-notification")));
      wait.until(
          ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-notification")));

      // Navigate directly back to the show detail view
      log.info("Navigating back to show detail view");
      driver.get(
          "http://localhost:"
              + serverPort
              + getContextPath()
              + "/show-detail/"
              + showService.findByName(showName).get(0).getId());

      // Verify navigation to the show detail view
      log.info("Waiting for show detail URL again");
      wait.until(ExpectedConditions.urlContains("/show-detail"));

      log.info("Waiting for grid to populate");
      wait.until(
          driver -> {
            List<WebElement> elements =
                driver.findElements(
                    By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)"));
            return elements.size() > 60 ? elements : null;
          });

      // Click the edit button on the first row
      log.info("Clicking edit segment button");
      WebElement editButton =
          driver.findElement(
              By.id(
                  "edit-segment-button-" + segmentService.getSegmentsByShow(show).get(0).getId()));
      clickAndScrollIntoView(editButton);

      // Wait for the dialog to appear
      log.info("Waiting for edit dialog");
      wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

      // Edit the description
      log.info("Editing summary");
      String newDescription = "This is the new description.";
      WebElement summaryField =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("edit-summary-text-area")));
      summaryField.sendKeys(newDescription, Keys.TAB);

      // Click the save button
      log.info("Clicking save button");
      WebElement saveButton = driver.findElement(By.id("edit-segment-save-button"));
      clickAndScrollIntoView(saveButton);

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      log.info("Waiting for save button to disappear");
      wait.withTimeout(Duration.ofSeconds(30));
      wait.until(
          ExpectedConditions.invisibilityOfElementLocated(By.id("edit-segment-save-button")));
      wait.withTimeout(Duration.ofSeconds(20));

      // Navigate back to the list.
      log.info("Navigating back to show detail view");
      driver.get(
          "http://localhost:"
              + serverPort
              + getContextPath()
              + "/show-detail/"
              + showService.findByName(showName).get(0).getId());

      // Verify the description has been updated
      log.info("Verifying description update");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.xpath("//vaadin-grid-cell-content[contains(., '" + newDescription + "')]"),
              newDescription));
    } catch (Exception e) {
      log.error("Error during E2E test", e);
      Assertions.fail(e);
    }
  }

  @Test
  public void testChangeSegmentOrder() {
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId());

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment mainEventSegment = new Segment();
    mainEventSegment.setNarration("This is a test narration.");
    mainEventSegment.setSummary("This is a test summary.");
    mainEventSegment.setSegmentOrder(1);
    mainEventSegment.setShow(show);
    mainEventSegment.setSegmentDate(Instant.now());
    mainEventSegment.setIsTitleSegment(true);
    mainEventSegment.setIsNpcGenerated(false);
    mainEventSegment.syncParticipants(Arrays.asList(wrestlers.get(2), wrestlers.get(3)));
    mainEventSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().get(0)));
    mainEventSegment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    mainEventSegment.setWinners(Arrays.asList(wrestlers.get(3)));
    HashSet<Title> titles = new HashSet<>();
    if (!titleService.getActiveTitles().isEmpty()) {
      titles.add(titleService.getActiveTitles().get(0));
    }
    mainEventSegment.setTitles(titles);
    segmentRepository.save(mainEventSegment);

    Segment firstSegment = new Segment();
    firstSegment.setNarration("This is a test narration.");
    firstSegment.setSummary("This is a test summary.");
    firstSegment.setSegmentOrder(2);
    firstSegment.setShow(show);
    firstSegment.setSegmentDate(Instant.now());
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().get(0)));
    firstSegment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentRepository.save(firstSegment);

    // Navigate to the Show List view
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Click on the newly created show in the grid to navigate to its detail page
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    clickAndScrollIntoView(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Wait for the segments to be visible
    wait.until(
        ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));

    // Click the down button on the first row
    WebElement downButton =
        driver.findElement(By.id("move-segment-down-button-" + mainEventSegment.getId()));
    clickAndScrollIntoView(downButton);

    // Navigate to the Show Detail view
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + show.getId());

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Moved up
    Assertions.assertNotNull(firstSegment.getId());
    Assertions.assertEquals(
        1, segmentRepository.findById(firstSegment.getId()).get().getSegmentOrder());
    // Moved down
    Assertions.assertNotNull(mainEventSegment.getId());
    Assertions.assertEquals(
        2, segmentRepository.findById(mainEventSegment.getId()).get().getSegmentOrder());
  }

  @Test
  public void testNarrateAndSummarizeSegment() {
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId());

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment firstSegment = new Segment();
    firstSegment.setNarration("Rob Van Dam looking for another dominant performance.");
    firstSegment.setSummary("");
    firstSegment.setSegmentOrder(1);
    firstSegment.setShow(show);
    firstSegment.setSegmentDate(Instant.now());
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().get(0)));
    firstSegment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentRepository.save(firstSegment);

    // Navigate to the Show List view
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Click on the newly created show in the grid to navigate to its detail page
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    clickAndScrollIntoView(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    WebElement narrateButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("generate-narration-button-" + firstSegment.getId())));
    clickAndScrollIntoView(narrateButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    WebElement generateNarrationButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("generate-narration-button")));
    clickAndScrollIntoView(generateNarrationButton);

    WebElement saveNarrationButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-narration-button")));
    clickAndScrollIntoView(saveNarrationButton);

    // Wait for the dialog to disappear
    WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    longWait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    WebElement summaryButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("generate-summary-button-" + firstSegment.getId())));
    clickAndScrollIntoView(summaryButton);

    // Navigate to the Show Detail view
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + show.getId());

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    List<WebElement> cells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));
    Assertions.assertEquals(22, cells.size()); // 11 headers, 1 rows
  }

  @Test
  public void testSetMainEvent() {
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId());

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment firstSegment = new Segment();
    firstSegment.setNarration("Rob Van Dam looking for another dominant performance.");
    firstSegment.setSummary("");
    firstSegment.setSegmentOrder(1);
    firstSegment.setShow(show);
    firstSegment.setSegmentDate(Instant.now());
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().get(0)));
    firstSegment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentRepository.save(firstSegment);

    // Navigate back to the list.
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Go to the show detailed view again to verify approved segments.
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    clickAndScrollIntoView(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Click the main event checkbox on the last row
    WebElement mainEventCheckbox =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("main-event-checkbox")));
    clickAndScrollIntoView(mainEventCheckbox);
    scrollIntoView(mainEventCheckbox);
  }
}

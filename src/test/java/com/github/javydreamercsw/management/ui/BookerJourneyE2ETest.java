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
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
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
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BookerJourneyE2ETest extends AbstractE2ETest {
  private static final String SHOW_TYPE_NAME = "Weekly";
  private static final String SEASON_NAME = "Test Season";
  private static final String TEMPLATE_NAME = "Continuum";

  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private SegmentService segmentService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired private CampaignEncounterRepository campaignEncounterRepository;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;

  @BeforeEach
  public void setupTestData() {
    cleanupLeagues();
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();
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
      Wrestler w = TestUtils.createWrestler("Wrestler " + i);
      wrestlerRepository.saveAndFlush(w);
    }
  }

  @Test
  @Tag("video")
  public void testFullShowLifecycle() {
    setVideoInfo("Booker Journey", "Full Show Lifecycle", "booker-full-show-lifecycle");
    try {
      // Navigate to the Show List view
      log.info("Navigating to show list");
      navigateTo("show-list");
      waitForVaadinClientToLoad();
      captureCaption(
          "Show List — the starting point for every show. To create a new show, fill in the"
              + " name, pick a Show Type (Weekly, PPV, etc.), assign a Season, choose a Template,"
              + " and set the date. Each field shapes what the AI will plan for you.",
          4000);

      final String showName = "My E2E Show";

      // Click the "Create" button
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

      // Fill in the form
      Objects.requireNonNull(
              wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("show-name"))))
          .sendKeys(showName);
      WebElement showTypeComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("show-type")));
      WebElement seasonComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("season")));
      WebElement templateComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("show-template")));

      selectFromVaadinComboBox(showTypeComboBox, SHOW_TYPE_NAME);
      captureCaption(
          "Show Type controls pacing expectations — 'Weekly' tells the AI to propose a"
              + " balanced card with a mix of matches and promos. PPV types lean heavier on"
              + " title matches and longer main events.",
          3500);

      wait.until(driver -> templateComboBox.isEnabled());

      selectFromVaadinComboBox(seasonComboBox, SEASON_NAME);
      captureCaption(
          "Season links this show to its storyline arc. The AI will factor in current feuds,"
              + " title reigns, and fan momentum within the season when building the card.",
          3000);

      selectFromVaadinComboBox(templateComboBox, TEMPLATE_NAME);
      captureCaption(
          "Templates define the show's structure — segment count, expected match types, and"
              + " promo slots. The 'Continuum' template is a balanced weekly format with"
              + " 5 matches and 2 promos.",
          3500);

      WebElement universeComboBox =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("universe")));
      selectFromVaadinComboBox(universeComboBox, "Default Universe");

      driver
          .findElement(By.id("show-date"))
          .sendKeys(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));

      // Click the "Create" button
      log.info("Creating show");
      WebElement createButton = driver.findElement(By.id("create-show-button"));
      clickElement(createButton);

      log.info("Waiting for show to appear in grid");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(By.tagName("vaadin-grid"), showName));

      List<Show> matchingShows = showService.findByName(showName);
      Assertions.assertEquals(1, matchingShows.size());
      Show show = matchingShows.getFirst();

      captureCaption(
          "Show created and listed — the grid shows all scheduled shows with their type,"
              + " season, and status. Click 'View Details' to open the show and start planning.",
          3000);

      // Click on the newly created show in the grid to navigate to its detail page
      log.info("Navigating to show detail page");
      WebElement viewShowDetails =
          wait.until(
              ExpectedConditions.elementToBeClickable(
                  By.id("view-details-button-" + show.getId())));
      Assertions.assertNotNull(viewShowDetails);
      clickElement(viewShowDetails);

      // Verify navigation to the show detail view (or planning view)
      log.info("Waiting for show detail URL");
      wait.until(ExpectedConditions.urlContains("/show-detail"));

      // Expand the details to see the plan button
      log.info("Expanding show details");
      WebElement showInfoDetails =
          wait.until(ExpectedConditions.presenceOfElementLocated(By.id("show-info-details")));
      clickElement(showInfoDetails);

      captureCaption(
          "Show Detail view — the info panel shows metadata and actions. From here you can:"
              + " plan the card with AI (Plan Show), manually add segments, or finalize the show"
              + " once all segments are set. Click 'Plan Show' to let the AI build the card.",
          4000);

      // Click the "Planning Show" button
      log.info("Navigating to show planning view");
      WebElement planningShowButton =
          wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("plan-show-button")));
      Assertions.assertNotNull(planningShowButton);
      clickElement(planningShowButton);

      // Verify navigation to the show planning view
      log.info("Waiting for show planning URL");
      wait.until(ExpectedConditions.urlContains("/show-planning"));

      log.info("Waiting for show planning context area");
      WebElement showPlanningContextArea =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("show-planning-context-area")));
      wait.until(
          driver -> {
            Assertions.assertNotNull(showPlanningContextArea);
            return !showPlanningContextArea.getText().isEmpty();
          });
      Assertions.assertNotNull(showPlanningContextArea);
      Assertions.assertFalse(showPlanningContextArea.getText().contains("Error"));

      captureCaption(
          "Show Planning view — the AI context panel on the left summarizes the roster:"
              + " active feuds, title reigns, fan momentum, and available wrestlers."
              + " This is the context sent to the AI when you click 'Propose Segments'."
              + " You can also add segments manually using the Add Segment button.",
          4500);

      // Click the "Propose Segments" button
      log.info("Proposing segments");
      WebElement proposeSegmentsButton =
          wait.until(ExpectedConditions.elementToBeClickable(By.id("propose-segments-button")));
      Assertions.assertNotNull(proposeSegmentsButton);
      clickElement(proposeSegmentsButton);

      log.info("Waiting for proposed segments grid");
      wait.until(
          ExpectedConditions.presenceOfElementLocated(
              By.cssSelector("vaadin-grid#proposed-segments-grid vaadin-grid-cell-content")));

      captureCaption(
          "AI-proposed card — each row is a suggested segment with match type, participants,"
              + " and the reasoning behind the booking. You can accept all proposals at once"
              + " with Approve, or remove individual segments before approving."
              + " The AI picks match types from the available segment types in your library.",
          4500);

      // Approve segments
      log.info("Approving segments");
      WebElement approveButton =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("approve-segments-button")));
      Assertions.assertNotNull(approveButton);
      clickElement(approveButton);

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
              + showService.findByName(showName).getFirst().getId());

      // Verify navigation to the show detail view
      log.info("Waiting for show detail URL again");
      wait.until(ExpectedConditions.urlContains("/show-detail"));
      waitForVaadinClientToLoad();

      log.info("Waiting for segments grid (URL: {})", driver.getCurrentUrl());
      // segments-grid may be hidden (no segments yet) but present in DOM
      waitForVaadinElement(driver, By.id("segments-grid-wrapper"));
      waitForGridToPopulate("segments-grid");

      captureCaption(
          "Segments are now locked in on the show card. Each row shows the match type,"
              + " participants, and current summary. Available actions per segment:"
              + " Edit (change participants or summary), Narrate (AI writes the full match story),"
              + " Reorder (move it up or down the card), and Mark as Main Event.",
          4500);

      // Click the edit button on the first row
      log.info("Clicking edit segment button");
      WebElement editButton =
          wait.until(
              ExpectedConditions.elementToBeClickable(
                  By.id(
                      "edit-segment-button-"
                          + segmentService.getSegmentsByShow(show).get(0).getId())));
      clickElement(editButton);

      // Wait for the dialog to appear
      log.info("Waiting for edit dialog");
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

      captureCaption(
          "Segment editor — update the summary, swap participants, set the winner,"
              + " attach titles on the line, or change the match rules (Normal, No DQ, etc.)."
              + " All changes are saved immediately when you click Save.",
          4000);

      // Edit the description
      log.info("Editing summary");
      String newDescription = "This is the new description.";
      WebElement summaryField =
          wait.until(
              ExpectedConditions.visibilityOfElementLocated(By.id("edit-summary-text-area")));
      Assertions.assertNotNull(summaryField);
      clearField(summaryField);
      summaryField.sendKeys(newDescription, Keys.TAB);

      // Click the save button
      log.info("Clicking save button");
      WebElement saveButton = driver.findElement(By.id("edit-segment-save-button"));
      clickElement(saveButton);

      sleep(1000);

      log.info("Waiting for save button to disappear");
      wait.withTimeout(Duration.ofMinutes(1));
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
              + showService.findByName(showName).getFirst().getId());

      // Verify the description has been updated
      log.info("Verifying description update");
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(
              By.xpath("//vaadin-grid-cell-content[contains(., '" + newDescription + "')]"),
              newDescription));
      captureCaption(
          "Summary updated and reflected on the show card. Once you're happy with the card,"
              + " you can finalize the show — which locks the card, triggers fan updates,"
              + " and makes the results visible to players and viewers.",
          4000);
    } catch (Exception e) {
      log.error("Error during E2E test", e);
      Assertions.fail(e);
    }
  }

  @Test
  @Tag("video")
  public void testChangeSegmentOrder() {
    setVideoInfo("Booker Journey", "Change Segment Order", "booker-change-segment-order");
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId(),
            null,
            null,
            null,
            null);

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment mainEventSegment =
        segmentService.createSegment(
            show,
            segmentTypeRepository.findByName("One on One").get(),
            Instant.now(),
            new HashSet<>());
    mainEventSegment.setNarration("This is a test narration.");
    mainEventSegment.setSummary("This is a test summary.");
    mainEventSegment.setSegmentOrder(1);
    mainEventSegment.setIsTitleSegment(true);
    mainEventSegment.setIsNpcGenerated(false);
    mainEventSegment.syncParticipants(Arrays.asList(wrestlers.get(2), wrestlers.get(3)));
    mainEventSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().getFirst()));
    mainEventSegment.setWinners(Arrays.asList(wrestlers.get(3)));
    HashSet<Title> titles = new HashSet<>();
    if (!titleService.getActiveTitles().isEmpty()) {
      titles.add(titleService.getActiveTitles().getFirst());
    }
    mainEventSegment.setTitles(titles);
    segmentService.updateSegment(mainEventSegment);

    Segment firstSegment =
        segmentService.createSegment(
            show,
            segmentTypeRepository.findByName("One on One").get(),
            Instant.now(),
            new HashSet<>());
    firstSegment.setNarration("This is a test narration.");
    firstSegment.setSummary("This is a test summary.");
    firstSegment.setSegmentOrder(2);
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().getFirst()));
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentService.updateSegment(firstSegment);

    // Navigate to the Show List view
    navigateTo("show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Click on the newly created show in the grid to navigate to its detail page
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    Assertions.assertNotNull(viewShowDetails);
    captureCaption(
        "Show List — locate the show whose segment order needs adjusting and click"
            + " View Details to open the segment planning view.",
        3500);
    clickElement(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Wait for the segments to be visible
    wait.until(
        ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));

    captureCaption(
        "Show Detail — segments appear in their current scheduled order. Use the ↑ ↓"
            + " arrow buttons on each row to reposition them. The new order is saved"
            + " immediately without a separate save step.",
        4500);

    // Click the down button on the first row
    WebElement downButton =
        driver.findElement(By.id("move-segment-down-button-" + mainEventSegment.getId()));
    clickElement(downButton);

    // Navigate to the Show Detail view
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + show.getId());

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    wait.until(
        ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));
    captureCaption(
        "Order updated — the opener is now in position 1 and the main event has moved to"
            + " position 2. Adjustments take effect instantly and are reflected in the show"
            + " card, match sequence, and AI narration context.",
        4500);

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
  @Tag("video")
  public void testNarrateAndSummarizeSegment() {
    setVideoInfo(
        "Booker Journey", "Narrate and Summarize Segment", "booker-narrate-summarize-segment");
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId(),
            null,
            null,
            null,
            null);

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment firstSegment =
        segmentService.createSegment(
            show,
            segmentTypeRepository.findByName("One on One").get(),
            Instant.now(),
            new HashSet<>());
    firstSegment.setNarration("Rob Van Dam looking for another dominant performance.");
    firstSegment.setSummary("This is a test summary.");
    firstSegment.setSegmentOrder(1);
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().getFirst()));
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentService.updateSegment(firstSegment);

    // Navigate to the Show List view
    navigateTo("show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // Click on the newly created show in the grid to navigate to its detail page
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    captureCaption("Lets use AI to generate compelling narration for our segments.");
    clickElement(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("segments-grid-wrapper"));
    waitForGridToPopulate("segments-grid");

    WebElement narrateButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("generate-narration-button-" + firstSegment.getId())));
    Assertions.assertNotNull(narrateButton);
    clickElement(narrateButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    captureCaption("Once AI is properly configured it can help you narrate segments.");

    WebElement generateNarrationButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("generate-narration-button")));
    Assertions.assertNotNull(generateNarrationButton);
    clickElement(generateNarrationButton);

    WebElement saveNarrationButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-narration-button")));
    Assertions.assertNotNull(saveNarrationButton);
    captureCaption(
        "Wrestler's decks are used as well as any provided input like participants, referees and"
            + " other NPCs.");
    clickElement(saveNarrationButton);

    // Wait for the narration dialog to close. We check the `opened` attribute on the dialog's
    // root element (set via setId("narration-dialog")) rather than generic CSS visibility,
    // because the vaadin-dialog host element delegates its visual rendering to
    // vaadin-dialog-overlay (teleported to <body>) and may not change its own display state.
    // The CSS selector "#narration-dialog[opened]" only matches while the dialog is open;
    // invisibilityOfElementLocated returns true when no element matches (attribute removed).
    WebDriverWait longWait = new WebDriverWait(driver, Duration.ofMinutes(1));
    longWait.until(
        ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("#narration-dialog[opened]")));

    WebElement summaryButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("generate-summary-button-" + firstSegment.getId())));
    Assertions.assertNotNull(summaryButton);
    captureCaption("Summary is generated from the full narration.");
    clickElement(summaryButton);

    // Navigate to the Show Detail view
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + show.getId());

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    List<WebElement> cells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));
    Assertions.assertNotNull(cells);
    Assertions.assertEquals(
        25,
        cells.size()); // 12 headers, 1 row (drag handle col has empty header; icon in data cell)
  }

  @Test
  @Tag("video")
  public void testSetMainEvent() {
    setVideoInfo("Booker Journey", "Set Main Event", "booker-set-main-event");
    Show show =
        showService.createShow(
            "My E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId(),
            null,
            null,
            null,
            null);

    // Create a new segment objects
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    Segment firstSegment =
        segmentService.createSegment(
            show,
            segmentTypeRepository.findByName("One on One").get(),
            Instant.now(),
            new HashSet<>());
    firstSegment.setNarration("Rob Van Dam looking for another dominant performance.");
    firstSegment.setSummary("");
    firstSegment.setSegmentOrder(1);
    firstSegment.setIsTitleSegment(false);
    firstSegment.setIsNpcGenerated(false);
    firstSegment.syncParticipants(Arrays.asList(wrestlers.get(0), wrestlers.get(1)));
    firstSegment.syncSegmentRules(Arrays.asList(segmentRuleService.findAll().getFirst()));
    firstSegment.setWinners(Arrays.asList(wrestlers.get(0)));
    segmentService.updateSegment(firstSegment);

    // Navigate back to the list.
    navigateTo("show-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Go to the show detailed view again to verify approved segments.
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    Assertions.assertNotNull(viewShowDetails);
    captureCaption(
        "Show List — click View Details on the target show to open the segment planning"
            + " view where the main event can be designated.",
        3500);
    clickElement(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Click the main event checkbox on the last row
    WebElement mainEventCheckbox =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("main-event-checkbox")));
    Assertions.assertNotNull(mainEventCheckbox);
    captureCaption(
        "Show Detail — each segment row has a Main Event checkbox. Checking it marks"
            + " this match as the headline bout, which boosts AI narration drama and"
            + " applies a fan multiplier to both winner and loser.",
        4500);
    clickElement(mainEventCheckbox);

    waitForVaadinClientToLoad();
    captureCaption(
        "Main event confirmed — the segment is now flagged as the night's headline."
            + " When the show is finalised, the winner earns elevated fan gains and the"
            + " AI narration treats this as the climax of the event.",
        4000);
  }
}

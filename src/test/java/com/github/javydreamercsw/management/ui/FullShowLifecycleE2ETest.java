package com.github.javydreamercsw.management.ui;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
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
import java.util.Optional;
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

public class FullShowLifecycleE2ETest extends AbstractE2ETest {
  private static final String SHOW_TYPE_NAME = "Weekly";
  private static final String SEASON_NAME = "Test Season";
  private static final String TEMPLATE_NAME = "Continuum";

  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private TitleRepository titleRepository;

  @BeforeEach
  @Transactional
  public void setupTestData() {
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

    // Clear and insert required ShowType
    Optional<ShowType> st = showTypeRepository.findByName(SHOW_TYPE_NAME);
    if (st.isEmpty()) {
      ShowType showType = new ShowType();
      showType.setName(SHOW_TYPE_NAME);
      showType.setDescription("A weekly show");
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
      TestUtils.createWrestler(wrestlerRepository, "Wrestler " + i);
    }
  }

  @Test
  public void testFullShowLifecycle() {
    // Navigate to the Show List view
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    final String showName = "My E2E Show";

    // Click the "Create" button
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Fill in the form
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("vaadin-text-field")))
        .sendKeys(showName);
    List<WebElement> comboBoxes = driver.findElements(By.cssSelector("vaadin-combo-box"));
    comboBoxes.get(0).sendKeys(SHOW_TYPE_NAME);
    comboBoxes.get(1).sendKeys(SEASON_NAME);
    comboBoxes.get(2).sendKeys(TEMPLATE_NAME);
    driver
        .findElement(By.id("show-date"))
        .sendKeys(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));

    // Click the "Create" button
    WebElement createButton = driver.findElement(By.id("create-show-button"));
    clickAndScrollIntoView(createButton);

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.tagName("vaadin-grid"), showName));

    List<Show> matchingShows = showService.findByName(showName);
    Assertions.assertEquals(1, matchingShows.size());
    Show show = matchingShows.get(0);

    // Click on the newly created show in the grid to navigate to its detail page
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    clickAndScrollIntoView(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    // Click the "Planning Show" button
    WebElement planningShowButton =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("plan-show-button")));
    clickAndScrollIntoView(planningShowButton);

    // Verify navigation to the show planning view
    wait.until(ExpectedConditions.urlContains("/show-planning"));

    WebElement showPlanningContextArea =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("show-planning-context-area")));
    Assertions.assertFalse(showPlanningContextArea.getText().isEmpty());

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("proposed-segments-grid")));

    // Approve segments
    WebElement approveButton =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("approve-segments-button")));
    clickAndScrollIntoView(approveButton);

    // Navigate back to the list.
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    // Go to the show detailed view again to verify approved segments.
    viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    clickAndScrollIntoView(viewShowDetails);

    // Verify navigation to the show detail view (or planning view)
    wait.until(ExpectedConditions.urlContains("/show-detail"));

    List<WebElement> cells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));
    Assertions.assertEquals(
        7 * 8 + 11, cells.size()); // 11 headers, 7 rows (8 of the columns have values)

    // Click the edit button on the first row
    WebElement editButton =
        driver.findElement(
            By.id("edit-segment-button-" + segmentService.getSegmentsByShow(show).get(0).getId()));
    clickAndScrollIntoView(editButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Edit the description
    String newDescription = "This is the new description.";
    WebElement summaryField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-summary-text-area")));
    summaryField.sendKeys(newDescription, Keys.TAB);

    // Click the save button
    WebElement saveButton = driver.findElement(By.id("edit-segment-save-button"));
    clickAndScrollIntoView(saveButton);

    // Wait for the dialog to appear
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Navigate back to the list.
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/show-detail/"
            + showService.findByName(showName).get(0).getId());

    // Verify the description has been updated
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.xpath("//vaadin-grid-cell-content[contains(., '" + newDescription + "')]"),
            newDescription));
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
    wait.until(
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
    WebElement mainEventCheckbox = driver.findElement(By.id("main-event-checkbox"));
    clickAndScrollIntoView(mainEventCheckbox);
    scrollIntoView(mainEventCheckbox);
  }
}

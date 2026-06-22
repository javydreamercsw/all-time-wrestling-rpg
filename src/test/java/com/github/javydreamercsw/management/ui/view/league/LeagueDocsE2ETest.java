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
package com.github.javydreamercsw.management.ui.view.league;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

// Static import needed

@Slf4j
public class LeagueDocsE2ETest extends AbstractE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private SeasonService seasonService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private GameSettingService gameSettingService; // Corrected injection

  private String seasonName; // Corrected placement

  @BeforeEach
  public void setupTest() {
    cleanupLeagues();
    ensurePlayerAccount();
    ensureWrestlers();
    ensureSeasonExists();
    ensureShowTemplateExists();
    // Navigate to a known page after DB reset so Vaadin rebuilds views with fresh data.
    // Without this, a view cached in the previous test's session may render stale state.
    navigateTo("leagues");
    waitForVaadinClientToLoad();
  }

  @Test
  void generateLeagueDocumentation() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Admin is already logged in from setup()
    navigateTo("leagues");

    // 1. League Creation
    waitForVaadinElement(driver, By.id("create-league-btn"));
    clickElement(By.id("create-league-btn"));
    waitForVaadinElement(driver, By.id("league-name-field"));

    documentFeature(
        "Leagues",
        "Creating a League",
        """
        Commissioners can create new leagues, setting the maximum picks per player and inviting\
         other players.\
        """,
        "league-creation");

    // Fill form to progress
    final String leagueName = "Docs League " + System.currentTimeMillis();
    WebElement nameField = driver.findElement(By.id("league-name-field"));
    nameField.sendKeys(leagueName);
    nameField.sendKeys(Keys.TAB);

    WebElement maxPicksField = driver.findElement(By.id("league-max-picks-field"));
    ((JavascriptExecutor) driver)
        .executeScript(
            """
            arguments[0].value = 1; arguments[0].dispatchEvent(new CustomEvent('input', { bubbles:\
             true })); arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true\
             }));\
            """,
            maxPicksField);
    maxPicksField.sendKeys(Keys.TAB);

    clickElement(By.id("league-commissioner-plays-checkbox"));
    selectFromVaadinMultiSelectComboBox(driver.findElement(By.id("participants-combo")), "player1");
    clickElement(By.id("league-save-btn"));
    waitForNotification("League saved successfully");

    League league = leagueRepository.findByName(leagueName).orElseThrow();

    // 2. Draft Room
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));

    documentFeature(
        "Leagues",
        "The Draft Room",
        """
        Players participate in a snake draft to build their rosters. The UI shows the current turn\
         and available wrestlers.\
        """,
        "league-draft-room");

    // Complete Draft to show other features
    List<Wrestler> wrestlers =
        new java.util.ArrayList<>(
            wrestlerRepository.findAll().stream()
                .filter(w -> Boolean.TRUE.equals(w.getActive()))
                .toList());
    // Sort by name to match DraftView grid default order
    wrestlers.sort(java.util.Comparator.comparing(Wrestler::getName));

    Wrestler w1 = wrestlers.get(0);
    clickElement(By.id("draft-wrestler-btn-" + w1.getId()));

    logout();
    login("player1", "password123");
    navigateTo("leagues");
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));
    waitForVaadinClientToLoad();
    Wrestler w2 = wrestlers.get(1);
    clickElement(By.id("draft-wrestler-btn-" + w2.getId()));

    // Verify Draft Completed
    waitForPageSourceToContain("Draft Completed");

    // 3. Match Reporting
    // Create a show and segment first (as admin)
    logout();
    login("admin", "admin123");
    final String showName = "Docs Show " + System.currentTimeMillis();
    createLeagueShow(leagueName, showName);
    // showService.findByName now returns a List, so get the first element
    Show show = showService.findByName(showName).get(0);
    addSegmentToShow(show, w1.getName(), w2.getName()); // admin's pick vs player1's wrestler

    // Login as player to report
    logout();
    login("player1", "password123");
    navigateTo("inbox");
    waitForGridToPopulate("inbox-grid");

    WebElement inboxGrid = driver.findElement(By.id("inbox-grid"));
    WebElement reportButton =
        inboxGrid.findElement(By.cssSelector("vaadin-button[id^='report-result-btn-']"));
    clickElement(reportButton);
    waitForVaadinElement(driver, By.id("match-report-dialog"));

    documentFeature(
        "Leagues",
        "Match Reporting",
        """
        When a league match is booked, players receive a notification to report the results of\
         their matches.\
        """,
        "league-match-report");

    // Submit report
    WebElement winnerCombo = driver.findElement(By.id("match-winner-select"));
    selectFromVaadinComboBox(winnerCombo, w2.getName());
    clickElement(By.id("submit-match-result-btn"));
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("match-report-dialog")));

    // 4. League Dashboard
    // Finalize first
    logout();
    login("admin", "admin123");
    finalizeShow(show);

    navigateTo("leagues");
    waitForGridToPopulate("league-grid");
    clickElement(driver.findElement(By.xpath("//vaadin-button[text()='Dashboard']")));
    waitForVaadinElement(driver, By.tagName("vaadin-tabs"));

    documentFeature(
        "Leagues",
        "League Standings",
        """
        The league dashboard displays the current standings, including wins, losses, and draws for\
         each wrestler.\
        """,
        "league-standings");

    click("vaadin-tab", "Rosters");
    waitForPageSourceToContain("League Rosters");
    documentFeature(
        "Leagues",
        "League Rosters",
        "View the complete rosters for all players in the league.",
        "league-rosters-tab");

    click("vaadin-tab", "Show History");
    waitForPageSourceToContain("Show History");
    documentFeature(
        "Leagues",
        "League History",
        "Keep track of all shows and match results that occurred within the league.",
        "league-history-tab");
  }

  @Tag("video")
  @Test
  void testRecordDraftRoomWalkthrough() {
    setVideoInfo("Leagues", "The Draft Room", "draft-room-walkthrough");

    // Create a minimal league so we can reach the Draft Room
    navigateTo("leagues");
    waitForVaadinElement(driver, By.id("create-league-btn"));
    clickElement(By.id("create-league-btn"));
    waitForVaadinElement(driver, By.id("league-name-field"));

    final String leagueName = "Draft Docs League " + System.currentTimeMillis();
    WebElement nameField = driver.findElement(By.id("league-name-field"));
    nameField.sendKeys(leagueName);
    nameField.sendKeys(Keys.TAB);

    WebElement maxPicksField = driver.findElement(By.id("league-max-picks-field"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = 1;"
                + " arguments[0].dispatchEvent(new CustomEvent('input', { bubbles: true }));"
                + " arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true }));",
            maxPicksField);
    maxPicksField.sendKeys(Keys.TAB);

    clickElement(By.id("league-commissioner-plays-checkbox"));
    selectFromVaadinMultiSelectComboBox(driver.findElement(By.id("participants-combo")), "player1");
    clickElement(By.id("league-save-btn"));
    waitForNotification("League saved successfully");

    waitForGridToPopulate("league-grid");

    captureCaption(
        "League List — when a league shows PRE_DRAFT status, click the Draft Room button to"
            + " begin the snake draft. The commissioner controls the pick order and can start"
            + " the draft for all participants at once.",
        4500);

    League league = leagueRepository.findByName(leagueName).orElseThrow();
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));
    waitForVaadinClientToLoad();

    captureCaption(
        "Draft Room — the left panel lists all available wrestlers sorted alphabetically,"
            + " with their tier, stamina, and health. The header shows the current round and"
            + " pick number; the highlighted turn label shows whose pick it is.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 250)");
    sleep(800);
    captureCaption(
        "Each row has a Draft button — click it to claim that wrestler for your roster."
            + " The snake draft reverses pick order each round, so the last picker in"
            + " round 1 picks first in round 2, balancing the selection.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(600);

    // Make admin's pick
    List<Wrestler> wrestlers =
        new java.util.ArrayList<>(
            wrestlerRepository.findAll().stream()
                .filter(w -> Boolean.TRUE.equals(w.getActive()))
                .toList());
    wrestlers.sort(java.util.Comparator.comparing(Wrestler::getName));
    Wrestler w1 = wrestlers.get(0);
    captureCaption(
        "Click Draft to lock in your pick — the wrestler disappears from the available"
            + " pool instantly and the Draft History panel on the right records the pick"
            + " with round number, pick number, and the player who claimed them.",
        4000);
    clickElement(By.id("draft-wrestler-btn-" + w1.getId()));
    waitForVaadinClientToLoad();
    sleep(800);

    captureCaption(
        "Turn updated — the turn label now shows it's player1's pick. All participants"
            + " see the same live board simultaneously, so there is no need to refresh."
            + " Each pick resolves within seconds of the button click.",
        4500);

    sleep(1500);
  }

  @Tag("video")
  @Test
  void testRecordLeagueLifecycleWalkthrough() {
    setVideoInfo("Leagues", "League Season — Full Lifecycle", "league-lifecycle-walkthrough");

    navigateTo("leagues");
    waitForVaadinClientToLoad();

    captureCaption(
        "Leagues List — a promotion can run multiple simultaneous fantasy leagues. Each row"
            + " shows the league name, status (PRE_DRAFT → ACTIVE → COMPLETED), commissioner,"
            + " and max picks per player. The Create League button starts a new competition.",
        5000);

    // Show the Create League form
    waitForVaadinElement(driver, By.id("create-league-btn"));
    captureCaption(
        "Click Create League to set up a new competition. Configure the league name, how many"
            + " wrestlers each participant can draft, whether the commissioner plays, and which"
            + " accounts are invited as league members.",
        4500);

    clickElement(By.id("create-league-btn"));
    waitForVaadinElement(driver, By.id("league-name-field"));

    captureCaption(
        "Create League form — Name and Max Picks are required. Toggle 'Commissioner Plays'"
            + " if the person running the league also wants a draft roster. Use the Participants"
            + " combo to invite other accounts; they receive an inbox notification.",
        4500);

    // Create the league
    final String leagueName = "Lifecycle Docs League " + System.currentTimeMillis();
    WebElement nameField = driver.findElement(By.id("league-name-field"));
    nameField.sendKeys(leagueName);
    nameField.sendKeys(Keys.TAB);

    WebElement maxPicksField = driver.findElement(By.id("league-max-picks-field"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = 1;"
                + " arguments[0].dispatchEvent(new CustomEvent('input', { bubbles: true }));"
                + " arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true }));",
            maxPicksField);
    maxPicksField.sendKeys(Keys.TAB);
    clickElement(By.id("league-commissioner-plays-checkbox"));
    selectFromVaadinMultiSelectComboBox(driver.findElement(By.id("participants-combo")), "player1");
    clickElement(By.id("league-save-btn"));
    waitForNotification("League saved successfully");

    waitForGridToPopulate("league-grid");
    captureCaption(
        "League created — PRE_DRAFT status means the draft hasn't started yet. Open the"
            + " Draft Room to pick rosters, then book league matches on shows and have"
            + " players report results through their Inbox. The dashboard tracks standings.",
        4500);

    // Navigate into the draft room briefly to show it
    League league = leagueRepository.findByName(leagueName).orElseThrow();
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));
    waitForVaadinClientToLoad();

    captureCaption(
        "Draft Room — once all rosters are filled the league moves to ACTIVE status."
            + " Every drafted wrestler is tied to that league roster for the entire season;"
            + " the owner books them in matches and earns points from wins and fan growth.",
        4500);

    // Make one pick so the draft completes (max picks = 1, commissioner + player1)
    List<Wrestler> wrestlers =
        new java.util.ArrayList<>(
            wrestlerRepository.findAll().stream()
                .filter(w -> Boolean.TRUE.equals(w.getActive()))
                .toList());
    wrestlers.sort(java.util.Comparator.comparing(Wrestler::getName));
    Wrestler w1 = wrestlers.get(0);
    clickElement(By.id("draft-wrestler-btn-" + w1.getId()));
    waitForVaadinClientToLoad();
    sleep(600);

    // player1 picks
    logout();
    login("player1", "password123");
    navigateTo("leagues");
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));
    waitForVaadinClientToLoad();
    Wrestler w2 = wrestlers.get(1);
    clickElement(By.id("draft-wrestler-btn-" + w2.getId()));
    waitForPageSourceToContain("Draft Completed");
    sleep(600);

    // Back to admin to show dashboard
    logout();
    login("admin", "admin123");
    navigateTo("leagues");
    waitForGridToPopulate("league-grid");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement dashboardBtn =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[text()='Dashboard']")));
    captureCaption(
        "Once matches start being finalized the league moves to ACTIVE. Click Dashboard"
            + " at any time to review the current standings, player rosters, and full"
            + " show history for the season.",
        4000);
    clickElement(dashboardBtn);
    waitForVaadinElement(driver, By.tagName("vaadin-tabs"));

    captureCaption(
        "League Dashboard — Standings tab shows each wrestler's win/loss/draw record and"
            + " fan points accumulated through the season. Records update the moment a"
            + " show is finalized, keeping all participants in sync.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(800);
    captureCaption(
        "Switch to Rosters to see every player's drafted squad, or Show History to review"
            + " every finalized event and its outcomes. The complete season record is"
            + " preserved here after the league ends.",
        4000);

    sleep(1500);
  }

  private void ensurePlayerAccount() {
    if (accountRepository.findByUsername("player1").isEmpty()) {
      Account p1 =
          new Account("player1", passwordEncoder.encode("password123"), "player1@test.com");
      p1.addRole(roleRepository.findByName(RoleName.PLAYER).orElseThrow());
      accountRepository.save(p1);
    }
  }

  private void ensureWrestlers() {
    if (wrestlerRepository.count() < 10) {
      for (int i = 0; i < 10; i++) {
        wrestlerService.createWrestler(
            "Wrestler " + i,
            false,
            "Bio",
            com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MIDCARDER,
            null);
      }
    }
  }

  private void ensureSeasonExists() {
    LocalDate gameDate = gameSettingService.getCurrentGameDate();
    seasonName = "Docs Season " + gameDate.getYear() + "_" + System.currentTimeMillis();
    Season existingSeason = seasonService.findByName(seasonName);

    if (existingSeason == null) {
      Season season = new Season();
      season.setName(seasonName);
      season.setDescription("Season for documentation tests");
      season.setStartDate(gameDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
      season.setEndDate(gameDate.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      season.setShowsPerPpv(5);
      season.setIsActive(true);
      seasonService.save(season);
    }
  }

  private void ensureShowTemplateExists() {
    var weeklyType = showTypeService.findByName("Weekly");
    if (weeklyType.isEmpty()) {
      showTypeService.createOrUpdateShowType("Weekly", "Weekly Show", 5, 2);
    }
    if (showTemplateService.findByName("Continuum").isEmpty()) {
      showTemplateService.createOrUpdateTemplate("Continuum", "Default Template", "Weekly");
    }
  }

  private void createLeagueShow(final String leagueName, final String showName) {
    navigateTo("show-list");
    waitForVaadinElement(driver, By.id("show-name"));
    driver.findElement(By.id("show-name")).sendKeys(showName);

    selectFromVaadinComboBox("show-type", "Weekly");

    // Template is disabled until show-type is selected
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            d ->
                (Boolean)
                    ((JavascriptExecutor) d)
                        .executeScript(
                            "return !arguments[0].disabled;",
                            d.findElement(By.id("show-template"))));

    selectFromVaadinComboBox("season", seasonName);
    selectFromVaadinComboBox("show-template", "Continuum");
    selectFromVaadinComboBox("show-league", leagueName);

    driver
        .findElement(By.id("show-date"))
        .sendKeys(
            gameSettingService
                .getCurrentGameDate()
                .format(DateTimeFormatter.ofPattern("M/d/yyyy")));

    clickElement(By.id("create-show-button"));
    waitForPageSourceToContain("Show created.");
  }

  private void addSegmentToShow(
      final Show show, final String wrestlerName1, final String wrestlerName2) {
    WebElement viewShowDetails =
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(
                ExpectedConditions.elementToBeClickable(
                    By.id("view-details-button-" + show.getId())));
    clickElement(viewShowDetails);

    waitForVaadinElement(driver, By.id("segments-grid-wrapper"));
    clickElement(By.id("add-segment-btn"));
    waitForVaadinElement(driver, By.id("add-segment-dialog"));

    selectFromVaadinComboBox("segment-type-combo-box", "One on One");
    WebElement team1Combo = driver.findElement(By.id("add-team-combo-1"));
    selectFromVaadinMultiSelectComboBox(team1Combo, wrestlerName1);
    WebElement team2Combo = driver.findElement(By.id("add-team-combo-2"));
    selectFromVaadinMultiSelectComboBox(team2Combo, wrestlerName2);

    clickElement(By.id("add-segment-save-button"));
    waitForNotification("Segment added successfully!");
  }

  private void finalizeShow(final Show show) {
    navigateTo("show-list");
    waitForGridToPopulate("show-grid");
    clickElement(driver.findElement(By.id("show-name-button-" + show.getId())));
    waitForVaadinElement(driver, By.id("segments-grid"));
    clickElement(By.id("adjudicate-show-btn"));
    waitForNotification("Fan adjudication completed!");
  }
}

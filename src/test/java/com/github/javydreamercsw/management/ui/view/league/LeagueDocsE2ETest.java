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
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
        "Commissioners can create new leagues, setting the maximum picks per player and inviting"
            + " other players.",
        "league-creation");

    // Fill form to progress
    final String leagueName = "Docs League " + System.currentTimeMillis();
    WebElement nameField = driver.findElement(By.id("league-name-field"));
    nameField.sendKeys(leagueName);
    nameField.sendKeys(Keys.TAB);

    WebElement maxPicksField = driver.findElement(By.id("league-max-picks-field"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = 1; arguments[0].dispatchEvent(new CustomEvent('input', { bubbles:"
                + " true })); arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true"
                + " }));",
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
        "Players participate in a snake draft to build their rosters. The UI shows the current turn"
            + " and available wrestlers.",
        "league-draft-room");

    // Complete Draft to show other features
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    Random random = new Random();
    Wrestler w1 = wrestlers.get(0);
    clickElement(By.id("draft-wrestler-btn-" + w1.getId()));

    logout();
    login("player1", "password123");
    navigateTo("leagues");
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));
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
        "When a league match is booked, players receive a notification to report the results of"
            + " their matches.",
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
        "The league dashboard displays the current standings, including wins, losses, and draws for"
            + " each wrestler.",
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

  private void navigateTo(String route) {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/" + route);
    waitForVaadinClientToLoad();
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
      showTemplateService.createOrUpdateTemplate("Continuum", "Default Template", "Weekly", null);
    }
  }

  private void createLeagueShow(String leagueName, String showName) {
    navigateTo("show-list");
    waitForVaadinElement(driver, By.id("show-name"));
    driver.findElement(By.id("show-name")).sendKeys(showName);

    List<WebElement> comboBoxes = driver.findElements(By.cssSelector("vaadin-combo-box"));
    comboBoxes.get(0).sendKeys("Weekly", Keys.TAB);
    new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> comboBoxes.get(2).isEnabled());
    comboBoxes.get(1).sendKeys(seasonName, Keys.TAB);
    comboBoxes.get(2).sendKeys("Continuum", Keys.TAB);
    comboBoxes.get(3).sendKeys(leagueName, Keys.TAB);

    driver
        .findElement(By.id("show-date"))
        .sendKeys(
            gameSettingService
                .getCurrentGameDate()
                .format(DateTimeFormatter.ofPattern("M/d/yyyy"))); // Corrected to use game date

    clickElement(By.id("create-show-button"));
    waitForPageSourceToContain("Show created.");
  }

  private void addSegmentToShow(Show show, String wrestlerName1, String wrestlerName2) {
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
    WebElement wrestlersCombo = driver.findElement(By.id("wrestlers-combo-box"));
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, wrestlerName1);
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, wrestlerName2);

    clickElement(By.id("add-segment-save-button"));
    waitForNotification("Segment added successfully!");
  }

  private void finalizeShow(Show show) {
    navigateTo("show-list");
    waitForGridToPopulate("show-grid");
    clickElement(driver.findElement(By.id("show-name-button-" + show.getId())));
    waitForVaadinElement(driver, By.id("segments-grid"));
    clickElement(By.id("adjudicate-show-btn"));
    waitForNotification("Fan adjudication completed!");
  }
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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

@Slf4j
public class LeagueLifecycleE2ETest extends AbstractE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private LeagueMembershipRepository leagueMembershipRepository;
  @Autowired private SeasonService seasonService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeService showTypeService;

  @BeforeEach
  public void setupTest() {
    // Prerequisites
    ensurePlayerAccount();
    ensureWrestlers();
    ensureSeason();
    ensureShowTemplate();
  }

  @Test
  void testFullLeagueLifecycle() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Admin is already logged in from setup()
    navigateTo("leagues");

    // Step 1: League Creation (As Commissioner/Admin)
    waitForVaadinElement(driver, By.id("create-league-btn"));
    clickElement(By.id("create-league-btn"));

    // Fill form
    final String leagueName = "Hardening League " + new Date().getYear();
    WebElement nameField = waitForVaadinElement(driver, By.id("league-name-field"));
    nameField.sendKeys(leagueName);
    nameField.sendKeys(Keys.TAB); // Trigger blur

    // Set max picks to 2
    WebElement maxPicksField = driver.findElement(By.id("league-max-picks-field"));
    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = 2; arguments[0].dispatchEvent(new CustomEvent('input', { bubbles:"
                + " true })); arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true"
                + " }));",
            maxPicksField);
    maxPicksField.sendKeys(Keys.TAB);

    // Admin wants to play
    clickElement(By.id("league-commissioner-plays-checkbox"));

    // Select player1
    WebElement participantsCombo = waitForVaadinElement(driver, By.id("participants-combo"));
    selectFromVaadinMultiSelectComboBox(participantsCombo, "player1");

    // Save
    clickElement(By.id("league-save-btn"));
    waitForNotification("League saved successfully");

    // Verify league in grid
    waitForGridToPopulate("league-grid");
    assertGridContains("league-grid", leagueName);
    assertGridContains("league-grid", "PRE_DRAFT");

    // Verify league settings in DB
    League league = leagueRepository.findByName(leagueName).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(
        2, league.getMaxPicksPerPlayer(), "Max picks per player not saved correctly!");

    // Verify player1 is a member (Debug check)
    List<LeagueMembership> members = leagueMembershipRepository.findByLeague(league);
    boolean player1Joined =
        members.stream().anyMatch(m -> m.getMember().getUsername().equals("player1"));
    assertTrue(player1Joined, "Player1 did not join the league! Members: " + members.size());
    boolean adminJoined =
        members.stream().anyMatch(m -> m.getMember().getUsername().equals("admin"));
    assertTrue(
        adminJoined, "Admin did not join the league as a player! Members: " + members.size());

    // Step 2: The Snake Draft
    // Use the ID we added: league-draft-room-btn-<id>
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));

    // Verify draft header
    waitForPageSourceToContain("Round: 1 | Pick: 1");
    waitForPageSourceToContain("Current Turn: admin");

    // Draft a wrestler as admin
    // Find the first available 'Draft' button using the ID pattern
    WebElement availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    List<WebElement> draftButtons =
        availableWrestlersGrid.findElements(
            By.cssSelector("vaadin-button[id^='draft-wrestler-btn-']"));
    assertFalse(draftButtons.isEmpty(), "No draft buttons found");
    clickElement(draftButtons.get(0));

    // Verify turn change
    waitForPageSourceToContain("Current Turn: player1");

    // Login as player1 to continue draft
    logout();
    login("player1", "password123");

    navigateTo("leagues");
    clickElement(By.id("league-draft-room-btn-" + league.getId()));
    waitForVaadinElement(driver, By.id("draft-view"));

    // Verify player1 turn
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Current Turn: player1"));

    // Draft a wrestler as player1
    availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    draftButtons =
        availableWrestlersGrid.findElements(
            By.cssSelector("vaadin-button[id^='draft-wrestler-btn-']"));
    clickElement(draftButtons.get(0));

    // Snake draft: player1 gets another pick (Round 2)
    // Wait for UI to update (Round 2 | Pick: 1)
    waitForPageSourceToContain("Round: 2");
    assertTrue(driver.getPageSource().contains("Current Turn: player1"));

    // Draft second wrestler as player1
    availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    draftButtons =
        availableWrestlersGrid.findElements(
            By.cssSelector("vaadin-button[id^='draft-wrestler-btn-']"));
    clickElement(draftButtons.get(0));

    // Turn returns to admin
    new WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(
            d ->
                java.util.Objects.requireNonNull(d.getPageSource())
                    .contains("Current Turn: admin"));

    // Step 3: Booking a League Match (As Admin)
    logout();
    login("admin", "admin123");

    navigateTo("show-list");

    waitForVaadinElement(driver, By.id("show-name"));

    final String showName = "League Night 1";

    Objects.requireNonNull(
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("show-name"))))
        .sendKeys(showName);

    List<WebElement> comboBoxes = driver.findElements(By.cssSelector("vaadin-combo-box"));
    WebElement showTypeComboBox = comboBoxes.get(0);
    WebElement seasonComboBox = comboBoxes.get(1);
    WebElement templateComboBox = comboBoxes.get(2);
    WebElement leagueComboBox = comboBoxes.get(3);

    showTypeComboBox.sendKeys("Weekly", Keys.TAB);

    wait.until(driver -> templateComboBox.isEnabled());

    seasonComboBox.sendKeys("2025", Keys.TAB);
    templateComboBox.sendKeys("Continuum", Keys.TAB);
    leagueComboBox.sendKeys(leagueName, Keys.TAB);

    driver
        .findElement(By.id("show-date"))
        .sendKeys(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));

    clickElement(By.id("create-show-button"));
    waitForNotification("Show created.");

    // Navigate to Show Detail
    // Find the newly created show in the grid using the ID pattern
    List<Show> matchingShows = showService.findByName(showName);
    Assertions.assertEquals(1, matchingShows.size());
    Show show = matchingShows.get(0);

    // Click on the newly created show in the grid to navigate to its detail page
    log.info("Navigating to show detail page");
    WebElement viewShowDetails =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("view-details-button-" + show.getId())));
    Assertions.assertNotNull(viewShowDetails);
    clickElement(viewShowDetails);

    waitForVaadinElement(driver, By.id("segments-grid-wrapper"));

    // Add Segment
    clickElement(By.id("add-segment-btn"));
    waitForVaadinElement(driver, By.id("add-segment-dialog"));

    selectFromVaadinComboBox("segment-type-combo-box", "Match");

    WebElement wrestlersCombo = driver.findElement(By.id("wrestlers-combo-box"));

    selectFromVaadinMultiSelectComboBox(wrestlersCombo, "Rob Van Dam");

    String p1WrestlerName = getPlayer1WrestlerName();
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, p1WrestlerName);

    // Add another wrestler (can be admin's or anyone)
    // We need at least 2
    String adminWrestlerName = getAdminWrestlerName();
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, adminWrestlerName);

    clickElement(By.id("add-segment-save-button"));
    waitForNotification("Segment added successfully!");

    // Step 4: Player Reporting (As Player)
    logout();
    login("player1", "password123");

    navigateTo("inbox");
    waitForGridToPopulate("inbox-grid");

    // Find notification
    // Description contains "Pending match on show: League Night 1"
    assertGridContains("inbox-grid", "Pending match on show: League Night 1");

    // Click Report Result using ID pattern
    WebElement inboxGrid = driver.findElement(By.id("inbox-grid"));
    WebElement reportButton =
        inboxGrid.findElement(By.cssSelector("vaadin-button[id^='report-result-btn-']"));
    clickElement(reportButton);
    waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));

    // Select Winner
    WebElement winnerSelect = driver.findElement(By.id("match-winner-select"));

    ((JavascriptExecutor) driver).executeScript("arguments[0].opened = true;", winnerSelect);

    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].value = arguments[0].items[0];", winnerSelect);

    clickElement(By.id("submit-match-result-btn"));

    // Step 5: Finalization (As Commissioner)
    logout();
    login("admin", "admin123");

    navigateTo("show-list");
    waitForGridToPopulate("show-grid");
    WebElement finalShowGrid = driver.findElement(By.id("show-grid"));
    WebElement finalShowButton =
        finalShowGrid.findElement(By.cssSelector("vaadin-button[id^='show-name-button-']"));
    clickElement(finalShowButton);
    waitForVaadinElement(driver, By.id("segments-grid"));

    // We can just check if "SUBMITTED" text is in the grid row.
    assertGridContains("segments-grid", "SUBMITTED");

    // Click Adjudicate Fans
    clickElement(By.id("adjudicate-show-btn"));
    waitForNotification("Fan adjudication completed!");

    // Verify FINALIZED
    assertGridContains("segments-grid", "FINALIZED");
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
    // Ensure we have enough wrestlers
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

  private void ensureSeason() {
    if (seasonService.findByName("2025") == null) {
      seasonService.createSeason("2025", "Season 2025", 5);
    }
  }

  private void ensureShowTemplate() {
    // Ensure Weekly show type exists
    var weeklyType = showTypeService.findByName("Weekly");
    if (weeklyType.isEmpty()) {
      showTypeService.createOrUpdateShowType("Weekly", "Weekly Show", 5, 2);
    }

    if (showTemplateService.findByName("Continuum").isEmpty()) {
      showTemplateService.createOrUpdateTemplate("Continuum", "Default Template", "Weekly", null);
    }
  }

  private String getPlayer1WrestlerName() {
    Account p1 = accountRepository.findByUsername("player1").orElseThrow();
    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(p1);
    if (wrestlers.isEmpty()) return "Wrestler 1"; // Fallback
    return wrestlers.get(0).getName();
  }

  private String getAdminWrestlerName() {
    Account admin = accountRepository.findByUsername("admin").orElseThrow();
    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    if (wrestlers.isEmpty()) return "Wrestler 0"; // Fallback
    return wrestlers.get(0).getName();
  }

  private void waitForPageSourceToContain(String text) {
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(d -> Objects.requireNonNull(d.getPageSource()).contains(text));
  }
}

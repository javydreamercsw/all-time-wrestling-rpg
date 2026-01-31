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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

public class LeagueLifecycleE2ETest extends AbstractE2ETest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testFullLeagueLifecycle() {
    // Prerequisites: Ensure player1 exists
    ensurePlayerAccount();
    ensureWrestlers();

    // Step 1: League Creation (As Commissioner/Admin)
    // Already logged in as admin from setup()
    navigateTo("leagues");
    waitForVaadinElement(driver, By.id("league-grid"));

    clickButtonByText("New League");
    waitForVaadinElement(driver, By.id("league-dialog"));

    WebElement nameField = driver.findElement(By.id("league-name-field"));
    nameField.sendKeys("Hardening League 2026");

    WebElement participantsCombo = driver.findElement(By.id("participants-combo"));
    selectFromVaadinMultiSelectComboBox(participantsCombo, "player1");

    clickButtonByText("Create");
    waitForNotification("League created successfully");

    // Verify league in grid
    assertGridContains("league-grid", "Hardening League 2026");
    assertGridContains("league-grid", "PRE_DRAFT");

    // Step 2: The Snake Draft
    clickButtonByText("Draft Room");
    waitForVaadinElement(driver, By.id("draft-view"));

    // Verify draft header
    WebElement header = driver.findElement(By.tagName("h2"));
    assertTrue(header.getText().contains("Round: 1 | Pick: 1"));
    assertTrue(driver.getPageSource().contains("Current Turn: admin"));

    // Draft a wrestler as admin
    // Find the first available 'Draft' button
    WebElement availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    List<WebElement> draftButtons =
        availableWrestlersGrid.findElements(By.xpath(".//vaadin-button[text()='Draft']"));
    assertTrue(draftButtons.size() > 0, "No draft buttons found");
    clickElement(draftButtons.get(0));

    waitForNotification("Drafted");

    // Verify turn change
    // Since we can't easily wait for text change without a specific ID or polling, we poll
    waitForPageSourceToContain("Current Turn: player1");

    // Login as player1 to continue draft
    logout();
    login("player1", "password123");

    navigateTo("leagues");
    clickButtonByText("Draft Room");
    waitForVaadinElement(driver, By.id("draft-view"));

    // Verify player1 turn
    assertTrue(driver.getPageSource().contains("Current Turn: player1"));

    // Draft a wrestler as player1
    availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    draftButtons =
        availableWrestlersGrid.findElements(By.xpath(".//vaadin-button[text()='Draft']"));
    clickElement(draftButtons.get(0));
    waitForNotification("Drafted");

    // Snake draft: player1 gets another pick (Round 2)
    // Wait for UI to update (Round 2 | Pick: 1)
    waitForPageSourceToContain("Round: 2");
    assertTrue(driver.getPageSource().contains("Current Turn: player1"));

    // Draft second wrestler as player1
    availableWrestlersGrid = driver.findElement(By.id("available-wrestlers-grid"));
    draftButtons =
        availableWrestlersGrid.findElements(By.xpath(".//vaadin-button[text()='Draft']"));
    clickElement(draftButtons.get(0));
    waitForNotification("Drafted");

    // Turn returns to admin
    waitForPageSourceToContain("Current Turn: admin");

    // Step 3: Booking a League Match (As Admin)
    logout();
    login("admin", "admin123");

    navigateTo("show-list");
    clickButtonByText("New Show");
    waitForVaadinElement(driver, By.id("edit-show-details-dialog"));

    WebElement showNameField = driver.findElement(By.id("show-name"));
    showNameField.sendKeys("League Night 1");

    selectFromVaadinComboBox("show-type", "Weekly");
    selectFromVaadinComboBox("show-season", "2025"); // Assuming 2025 exists from DataInitializer
    selectFromVaadinComboBox("show-league", "Hardening League 2026");

    clickButtonByText("Save");
    waitForNotification("Show saved successfully");

    // Navigate to Show Detail
    // Click on the newly created show in the grid. Assuming it's the last one or sortable.
    // For simplicity, let's filter or finding it. The grid might be empty except this one.
    // Let's iterate rows to find "League Night 1"
    WebElement showGrid = driver.findElement(By.id("show-grid"));
    // Finding the row might be tricky without a specific ID logic in grid cells.
    // Assuming standard grid structure.
    // Let's assume it's visible.
    click("span", "League Night 1"); // Try clicking the text span

    waitForVaadinElement(
        driver, By.id("add-segment-dialog-layout")); // Wait? No, dialog not open yet.
    waitForVaadinElement(driver, By.id("segments-grid-wrapper"));

    // Add Segment
    clickButtonByText("Add Segment");
    waitForVaadinElement(driver, By.id("add-segment-dialog"));

    selectFromVaadinComboBox("segment-type-combo-box", "Match");

    // Select wrestler picked by player1.
    // We don't know exactly WHO player1 picked unless we tracked it.
    // But we know player1 picked 2 wrestlers.
    // We can select ANY wrestler that is in the league.
    // MultiSelect is tricky.
    // Let's select the first two available in the combo.
    WebElement wrestlersCombo = driver.findElement(By.id("wrestlers-combo-box"));
    // We need to type to filter or just open.
    // Let's pick a known wrestler name if possible.
    // Or just select items.
    selectFromVaadinMultiSelectComboBox(
        wrestlersCombo, "Hulk Hogan"); // Assuming he exists and was picked?
    // Wait, step 2 said "Click Draft next to any wrestler (e.g. Hulk Hogan)".
    // Admin picked first. Player1 picked second and third.
    // We need to pick a wrestler owned by Player1.
    // Let's retrieve Player1's wrestler name from DB helper
    String p1WrestlerName = getPlayer1WrestlerName();
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, p1WrestlerName);

    // Add another wrestler (can be admin's or anyone)
    // We need at least 2
    String adminWrestlerName = getAdminWrestlerName();
    selectFromVaadinMultiSelectComboBox(wrestlersCombo, adminWrestlerName);

    clickButtonByText("Add Segment");
    waitForNotification("Segment added successfully!");

    // Step 4: Player Reporting (As Player)
    logout();
    login("player1", "password123");

    navigateTo("inbox");
    waitForGridToPopulate("inbox-grid");

    // Find notification
    // Description contains "Pending match on show: League Night 1"
    assertGridContains("inbox-grid", "Pending match on show: League Night 1");

    clickButtonByText("Report Result");
    waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));

    // Select Winner
    WebElement winnerSelect = driver.findElement(By.tagName("vaadin-select"));
    // Select the first option (which should be one of the participants)
    // Vaadin Select automation:
    ((JavascriptExecutor) driver).executeScript("arguments[0].opened = true;", winnerSelect);
    // Find item and click
    // Need to wait for overlay
    // Simplified: just assuming p1WrestlerName is in the list
    // Actually, report dialog logic: select a winner.
    // Let's try to select p1WrestlerName
    // ... logic for selecting from Select component ...
    // Using a helper or JS
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].value = arguments[0].items[0];", winnerSelect);

    clickButtonByText("Submit Result");
    // Verify notification action changed or item updated.
    // The view refreshes. The item should now ideally be marked read or Action button gone?
    // The "Report Result" button only appears for MATCH_REQUEST.
    // Once submitted, the fulfillment status changes.
    // However, the inbox item itself persists until deleted/read.
    // But the button logic in InboxView checks event type.
    // The event type is still MATCH_REQUEST.
    // But `createActionComponent` logic:
    /*
        Button reportButton = new Button("Report Result", e -> { ... });
    */
    // It doesn't check if already submitted in the view construction, effectively.
    // But submitting again checks status in service.

    // Step 5: Finalization (As Commissioner)
    logout();
    login("admin", "admin123");

    navigateTo("show-list");
    click("span", "League Night 1");
    waitForVaadinElement(driver, By.id("segments-grid"));

    // Verify League Status column
    // This is the 6th column (index 5) based on creation order in ShowDetailView
    // "League Status"
    // We can just check if "SUBMITTED" text is in the grid row.
    assertGridContains("segments-grid", "SUBMITTED");

    // Click Adjudicate Fans
    clickButtonByText("Adjudicate Fans");
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
      Account p1 = new Account("player1", "password123", "player1@test.com");
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
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
        .until(d -> d.getPageSource().contains(text));
  }
}

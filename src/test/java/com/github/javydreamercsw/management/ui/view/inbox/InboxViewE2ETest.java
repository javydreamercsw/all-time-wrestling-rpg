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
package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InboxViewE2ETest extends AbstractE2ETest {

  @Autowired private InboxEventType fanAdjudication;
  @Autowired private InboxEventType rivalryHeatChange;
  @Autowired private WrestlerService wrestlerService;
  @Autowired protected TitleRepository titleRepository;
  @Autowired private AccountRepository accountRepository;

  private Wrestler w1;
  private Wrestler w2;
  private Account admin;

  @BeforeEach
  public void setUp() throws IOException {
    // Clear any existing inbox items to ensure a clean state for each test
    inboxRepository.deleteAll();
    w1 = createTestWrestler("Test Wrestler 1");
    wrestlerService.save(w1);
    w2 = createTestWrestler("Test Wrestler 2");
    wrestlerService.save(w2);
    admin = accountRepository.findByUsername("admin").orElseThrow();
  }

  @Test
  void testInboxViewLoads() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement grid =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
    // Assert that the grid is displayed
    Assertions.assertTrue(grid.isDisplayed());

    // Verify that the grid is initially empty, as DataInitializer does not create InboxItems.
    wait.until(ExpectedConditions.numberOfElementsToBe(By.tagName("vaadin-grid-row"), 0));
  }

  @Test
  void testFilterInboxItems() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setDescription("Filter Me Item");
    item1.setEventType(fanAdjudication);
    item1.setRead(false);
    item1.addTarget(w1.getId().toString(), InboxItemTarget.TargetType.WRESTLER);
    item1.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("Do Not Filter");
    item2.setEventType(rivalryHeatChange);
    item2.setRead(false);
    item2.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(item2);

    InboxItem item3 = new InboxItem();
    item3.setDescription("Filter Me Too");
    item3.setEventType(fanAdjudication);
    item3.setRead(true);
    item3.addTarget(w1.getId().toString(), InboxItemTarget.TargetType.WRESTLER);
    item3.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(item3);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");

    waitForVaadinToLoad(driver); // Wait for Vaadin to load
    waitForGridToPopulate("inbox-grid"); // Use helper to ensure grid is ready

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // Wait for one of the item descriptions to be visible in the grid using the new ID column
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("vaadin-grid"), "Filter Me Item"));

    // Verify initial items are loaded by counting non-empty vaadin-grid-cell-content elements
    // (including headers)

    List<WebElement> cells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));

    // 5 headers + 3 rows * 5 columns + 1 selection column = 24 cells
    Assertions.assertEquals(24, cells.size());

    // Explicitly set "Read Status" to "All" (this should already be the default, but we'll keep it
    // for robustness)

    WebElement toolbar =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".toolbar")));

    wait.until(
        ExpectedConditions.numberOfElementsToBeMoreThan(
            By.cssSelector(".toolbar vaadin-combo-box"), 1));

    assert toolbar != null;
    WebElement readStatusComboBox =
        toolbar.findElement(By.cssSelector("vaadin-combo-box:nth-of-type(2)"));

    selectFromVaadinComboBox(readStatusComboBox, "All");

    waitForVaadinToLoad(driver);

    // Verify items after setting filter to "All" (should still be 3)
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark as Read') or"
                    + " contains(text(), 'Mark as Unread')]"),
            3));

    WebElement filterField =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("inbox-target-filter")));

    filterField.click();
    filterField.sendKeys(w1.getName(), Keys.TAB);

    waitForVaadinToLoad(driver);
    // Verify filtered item appears
    List<WebElement> filteredDescriptionCells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath(
                    "//vaadin-grid-cell-content[contains(text(), 'Filter Me Item') or"
                        + " contains(text(), 'Filter Me Too')]")));
    assert filteredDescriptionCells != null;
    Assertions.assertEquals(2, filteredDescriptionCells.size());
    boolean foundItem1 = false;
    boolean foundItem3 = false;
    for (WebElement cell : filteredDescriptionCells) {
      if (cell.getText().contains("Filter Me Item")) {
        foundItem1 = true;
      }
      if (cell.getText().contains("Filter Me Too")) {
        foundItem3 = true;
      }
    }
    Assertions.assertTrue(
        foundItem1, "Expected 'Filter Me Item' to be present in filtered results.");
    Assertions.assertTrue(
        foundItem3, "Expected 'Filter Me Too' to be present in filtered results.");
  }

  @Test
  void testMassSelectAndDelete() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setDescription("Item 1");
    item1.setEventType(fanAdjudication);
    item1.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("Item 2");
    item2.setEventType(fanAdjudication);
    item2.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(item2);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForGridToPopulate("inbox-grid"); // Use the new helper method

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Select all using the ID we added
    WebElement selectAllCheckbox = waitForVaadinElement(driver, By.id("select-all-checkbox"));
    selectAllCheckbox.click();

    // Delete selected
    WebElement deleteSelectedButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[text()='Delete Selected']")));

    // Ensure no overlays are blocking (like notifications)
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-notification-card")));

    clickElement(deleteSelectedButton);

    // Verify grid is empty
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark Selected as"
                    + " Read') or contains(text(), 'Mark Selected as Unread')]"),
            0));
  }

  @Test
  void testMarkAsRead() {
    // Given
    InboxItem unreadItem = new InboxItem();
    unreadItem.setDescription("Unread Item");
    unreadItem.setEventType(fanAdjudication);
    unreadItem.setRead(false);
    unreadItem.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
    inboxRepository.saveAndFlush(unreadItem);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load
    waitForGridToPopulate("inbox-grid"); // Use helper to ensure grid is ready
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Verify initial item is loaded
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark as Read') or"
                    + " contains(text(), 'Mark as Unread')]"),
            1));

    // Select the first unread item
    WebElement firstRowCheckbox =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath(
                    "//vaadin-grid-cell-content[contains(text(), 'Unread"
                        + " Item')]//preceding-sibling::vaadin-grid-cell-content//vaadin-checkbox")));
    firstRowCheckbox.click();

    // Mark as read
    WebElement markAsReadButton =
        driver.findElement(By.xpath("//vaadin-button[text()='Mark Selected as Read']"));
    markAsReadButton.click();

    // Verify item is marked as read (button text changes to "Mark as Unread")
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath(
                "//vaadin-grid-cell-content[contains(text(), 'Unread"
                    + " Item')]//following-sibling::vaadin-grid-cell-content//vaadin-button[text()='Mark"
                    + " as Unread']")));
  }
}

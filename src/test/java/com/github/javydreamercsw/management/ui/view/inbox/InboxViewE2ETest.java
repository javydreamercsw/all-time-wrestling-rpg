package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Removed @SpringBootTest and @ActiveProfiles("test") as this is an E2E test and should not
// manage its own Spring context.
@Slf4j
class InboxViewE2ETest extends AbstractE2ETest {

  // Removed @Autowired private InboxRepository inboxRepository;
  // Access inboxRepository via AbstractE2ETest.inboxRepository

  @BeforeEach
  public void setUp() throws IOException {
    super.setup();
    // Clear any existing inbox items to ensure a clean state for each test
    inboxRepository.deleteAll();
  }

  @Test
  void testInboxViewLoads() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
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
    item1.setEventType("Test Event A");
    item1.setRead(false);
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("Do Not Filter");
    item2.setEventType("Test Event B");
    item2.setRead(false);
    inboxRepository.save(item2);

    InboxItem item3 = new InboxItem();
    item3.setDescription("Filter Me Too");
    item3.setEventType("Test Event A");
    item3.setRead(true);
    inboxRepository.save(item3);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");

    waitForVaadinToLoad(driver); // Wait for Vaadin to load

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for one of the item descriptions to be visible in the grid

    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("vaadin-grid"), "Filter Me Item"));

    // Verify initial items are loaded by counting non-empty vaadin-grid-cell-content elements
    // (including headers)

    List<WebElement> cells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("vaadin-grid > vaadin-grid-cell-content:not(:empty)")));

    Assertions.assertEquals(20, cells.size()); // 5 header cells + (3 rows * 5 columns) = 20 cells

    // Explicitly set "Read Status" to "All" (this should already be the default, but we'll keep it
    // for robustness)

    WebElement toolbar =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".toolbar")));

    wait.until(
        ExpectedConditions.numberOfElementsToBeMoreThan(
            By.cssSelector(".toolbar vaadin-combo-box"), 1));

    WebElement readStatusComboBox =
        toolbar.findElement(By.cssSelector("vaadin-combo-box:nth-of-type(2)"));

    readStatusComboBox.click();

    // Wait for the "All" option to be visible and clickable within the combo box overlay

    WebElement allOption =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-combo-box-overlay//vaadin-combo-box-item[normalize-space(.)='All']")));

    allOption.click();

    waitForVaadinToLoad(driver); // Wait for Vaadin to load after combo box selection

    // Verify items after setting filter to "All" (should still be 3)
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark as Read') or"
                    + " contains(text(), 'Mark as Unread')]"),
            3));

    // Apply filter
    WebElement filterField =
        driver.findElement(
            By.cssSelector("vaadin-text-field[placeholder='Filter by description...']"));
    filterField.sendKeys("Filter Me");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load after applying text filter

    // Verify filtered item appears
    List<WebElement> filteredDescriptionCells =
        wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath(
                    "//vaadin-grid-cell-content[contains(text(), 'Filter Me Item') or"
                        + " contains(text(), 'Filter Me Too')]")));
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
    item1.setEventType("Test Event");
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("Item 2");
    item2.setEventType("Test Event");
    inboxRepository.save(item2);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Verify initial items are loaded
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark as Read') or"
                    + " contains(text(), 'Mark as Unread')]"),
            2));

    // Select all
    WebElement selectAllCheckbox =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("vaadin-checkbox[aria-label='Select All']")));
    selectAllCheckbox.click();

    // Delete selected
    WebElement deleteSelectedButton =
        driver.findElement(By.xpath("//vaadin-button[text()='Delete Selected']"));
    deleteSelectedButton.click();

    // Verify grid is empty
    wait.until(
        ExpectedConditions.numberOfElementsToBe(
            By.xpath(
                "//vaadin-grid-cell-content//vaadin-button[contains(text(), 'Mark Selected as Read') or"
                    + " contains(text(), 'Mark Selected as Unread')]"),
            0));
  }

  @Test
  void testMarkAsRead() {
    // Given
    InboxItem unreadItem = new InboxItem();
    unreadItem.setDescription("Unread Item");
    unreadItem.setEventType("Test Event");
    unreadItem.setRead(false);
    inboxRepository.save(unreadItem);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/inbox");
    waitForVaadinToLoad(driver); // Wait for Vaadin to load
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

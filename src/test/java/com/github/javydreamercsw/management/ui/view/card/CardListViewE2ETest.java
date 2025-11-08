package com.github.javydreamercsw.management.ui.view.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.javydreamercsw.AbstractE2ETest;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CardListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testCreateCard() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Wait for the grid to be present and populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));

    // Find the input field and create button
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-text-field[placeholder='What do you want the card name to be?']")));
    nameField.sendKeys("New E2E Card");

    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    createButton.click();

    // Verify the new card is in the grid
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(20))
            .handleResultIf(result -> !((WebElement) result).getText().contains("New E2E Card"))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertTrue(refreshedGrid.getText().contains("New E2E Card"), refreshedGrid.getText());
              return refreshedGrid;
            });
  }

  @Test
  public void testUpdateCard() {
    String cardName = "Card to Update";
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Find the grid and wait for it to be populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Verify the new card is in the grid before trying to edit it
    RetryPolicy<Void> retryPolicyCreate =
        RetryPolicy.<Void>builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(20))
            .build();
    Failsafe.with(retryPolicyCreate)
        .run(
            () -> {
              driver.findElement(
                  By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
            });

    // Find the row for our card and click the edit button
    WebElement cardRow =
        grid.findElement(By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));

    WebElement editButton =
        cardRow.findElement(
            By.xpath(
                "./following-sibling::vaadin-grid-cell-content//vaadin-button[text()='Edit']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", editButton);
    wait.until(ExpectedConditions.elementToBeClickable(editButton));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editButton);

    // The editor is now open. Find the name field, update it, and save.
    WebElement editorNameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-text-field[data-testid='name-editor']")));
    String updatedName = cardName + " - Updated";
    wait.until(ExpectedConditions.elementToBeClickable(editorNameField));
    ((JavascriptExecutor) driver).executeScript("arguments[0].value = ''", editorNameField);
    editorNameField.sendKeys(updatedName);

    WebElement saveButton = grid.findElement(By.xpath("//vaadin-button[text()='Save']"));
    wait.until(ExpectedConditions.elementToBeClickable(saveButton));
    saveButton.click();

    // Verify the update
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handleResultIf(result -> !((WebElement) result).getText().contains(updatedName))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertTrue(refreshedGrid.getText().contains(updatedName), refreshedGrid.getText());
              return refreshedGrid;
            });
  }

  @Test
  public void testDeleteCard() {
    String cardName = "Card to Delete";

    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Find the grid and wait for it to be populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Find the row for our card and click the delete button
    WebElement cardRow = null;
    for (int i = 0; i < 20; i++) { // Try 20 times
      try {
        cardRow =
            grid.findElement(
                By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
        break; // Element found, exit loop
      } catch (org.openqa.selenium.NoSuchElementException e) {
        ((JavascriptExecutor) driver)
            .executeScript(
                "arguments[0].scrollIntoView(true);",
                grid.findElement(By.xpath("//vaadin-grid-cell-content[last()]")));
        try {
          Thread.sleep(500); // Wait for content to load
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    if (cardRow == null) {
      fail("Card with name '" + cardName + "' not found in the grid after scrolling.");
    }
    WebElement deleteButton =
        cardRow.findElement(
            By.xpath(
                "./following-sibling::vaadin-grid-cell-content//vaadin-button[@data-testid='delete-button']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteButton);

    // The confirmation dialog should appear. Click "Delete".
    WebElement confirmDeleteButton =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-confirm-dialog-overlay vaadin-button[slot='confirm-button']")));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmDeleteButton);

    // Verify the card is gone
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handleResultIf(result -> ((WebElement) result).getText().contains(cardName))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertFalse(
                  refreshedGrid.getText().contains(cardName),
                  "Card should have been deleted but was found: " + refreshedGrid.getText());
              return refreshedGrid;
            });
  }

  @Test
  public void testSortByName() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Wait for the grid to be present and populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Get the header cell for the "Name" column
    WebElement nameHeader = grid.findElement(By.xpath("//vaadin-grid-sorter[contains(., 'Name')]"));

    // Get the initial order of names
    List<String> initialOrder = getColumnData(grid, 0);

    // Click to sort ascending
    nameHeader.click();
    wait.until(ExpectedConditions.attributeContains(nameHeader, "direction", "asc"));
    List<String> ascOrder = getColumnData(grid, 0);
    List<String> sortedInitial = new ArrayList<>(initialOrder);
    Collections.sort(sortedInitial);
    assertEquals(sortedInitial, ascOrder);

    // Click to sort descending
    nameHeader.click();
    wait.until(ExpectedConditions.attributeContains(nameHeader, "direction", "desc"));
    List<String> descOrder = getColumnData(grid, 0);
    Collections.reverse(sortedInitial);
    assertEquals(sortedInitial, descOrder);
  }

  private List<String> getColumnData(WebElement grid, int columnIndex) {
    List<WebElement> cells =
        grid.findElements(
            By.xpath(
                "//vaadin-grid-cell-content[count(ancestor::vaadin-grid-column) = "
                    + (columnIndex + 1)
                    + "]"));
    return cells.stream().map(WebElement::getText).collect(Collectors.toList());
  }
}

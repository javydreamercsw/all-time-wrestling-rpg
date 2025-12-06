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
package com.github.javydreamercsw.management.ui.view.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CardListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testCreateCard() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.pollingEvery(Duration.ofMillis(500));

    // Find the input field and create button
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-text-field[placeholder='What do you want the card name to be?']")));
    nameField.sendKeys("New E2E Card");

    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    createButton.click();

    // Verify the new card is in the grid by scrolling to the end.
    findCardInGridWithScrolling(wait, "New E2E Card");
  }

  @Test
  public void testUpdateCard() {
    String cardName = "Card to Update";
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.pollingEvery(Duration.ofMillis(500));

    // Find the input field and create button
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-text-field[placeholder='What do you want the card name to be?']")));
    nameField.sendKeys(cardName);

    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    createButton.click();

    // Find the row for our card
    WebElement cardRow = findCardInGridWithScrolling(wait, cardName);

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

    WebElement grid = driver.findElement(By.tagName("vaadin-grid"));
    WebElement saveButton = grid.findElement(By.xpath("//vaadin-button[text()='Save']"));
    wait.until(ExpectedConditions.elementToBeClickable(saveButton));
    saveButton.click();

    // Verify the update
    findCardInGridWithScrolling(wait, updatedName);
  }

  @Test
  public void testDeleteCard() {
    String cardName = "Card to Delete";

    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.pollingEvery(Duration.ofMillis(500));

    // Find the input field and create button
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-text-field[placeholder='What do you want the card name to be?']")));
    nameField.sendKeys(cardName);

    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    createButton.click();

    // Find the row for our card
    WebElement cardRow = findCardInGridWithScrolling(wait, cardName);

    WebElement deleteButton =
        cardRow.findElement(
            By.xpath(
                "./following-sibling::vaadin-grid-cell-content//vaadin-button[@data-testid='delete-button']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteButton);

    // The confirmation dialog should appear. Click "Delete".
    WebElement dialogOverlay =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.tagName("vaadin-confirm-dialog-overlay")));

    WebElement confirmDeleteButton =
        dialogOverlay.findElement(By.xpath(".//vaadin-button[text()='Delete']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmDeleteButton);

    // Verify the card is gone
    boolean notFound =
        wait.until(
            ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]")));
    assertTrue(notFound);
  }

  private WebElement findCardInGridWithScrolling(WebDriverWait wait, String cardName) {
    return wait.until(
        d -> {
          WebElement grid = d.findElement(By.tagName("vaadin-grid"));
          try {
            // Try to find without scrolling first
            return grid.findElement(
                By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
          } catch (NoSuchElementException e) {
            ((JavascriptExecutor) d).executeScript("arguments[0].scrollToIndex(999999)", grid);
            // After scrolling, try to find it again. If not found, wait will retry.
            return d.findElement(
                By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
          }
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
}

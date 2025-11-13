package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class WrestlerListViewE2ETest extends AbstractE2ETest {

  @Test
  void testCreateWrestler() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Get the initial size of the grid
    long initialSize = wrestlerRepository.count();

    // Click the "Create Wrestler" button
    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-wrestler-button")));
    clickAndScrollIntoView(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the components
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    // Enter a new wrestler name
    nameField.sendKeys("Test Wrestler", Keys.TAB);

    // Click the save button
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    clickAndScrollIntoView(saveButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Verify that the new wrestler appears in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Test Wrestler"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, wrestlerRepository.count());
  }

  @Test
  void testEditWrestler() {
    // Create a wrestler to edit
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Wrestler to Edit");
    wrestler.setDeckSize(10);
    wrestler.setLowHealth(0);
    wrestler.setStartingHealth(100);
    wrestler.setCurrentHealth(100);
    wrestler.setStartingStamina(100);
    wrestler.setLowStamina(0);
    wrestlerRepository.save(wrestler);
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    // Find the "Edit" button for the wrestler and click it
    WebElement editButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[@id='edit-" + wrestler.getId() + "']")));

    clickAndScrollIntoView(editButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the editor's name field and change the value
    WebElement nameEditor =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    nameEditor.sendKeys(" Updated", Keys.TAB);

    // Find the "Save" button and click it
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    clickAndScrollIntoView(saveButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Verify that the grid is updated
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Wrestler to Edit Updated"));
          } catch (Exception e) {
            return false;
          }
        });

    assertTrue(
        wrestlerRepository.findAll().stream()
            .anyMatch(w -> w.getName().equals("Wrestler to Edit Updated")));
  }

  @Test
  void testDeleteWrestler() {
    // Create a wrestler to delete
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Wrestler to Delete");
    wrestler.setDeckSize(10);
    wrestler.setLowHealth(0);
    wrestler.setStartingHealth(100);
    wrestler.setCurrentHealth(100);
    wrestler.setStartingStamina(100);
    wrestler.setLowStamina(0);

    wrestlerRepository.save(wrestler);
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    long initialSize = wrestlerRepository.count();

    // Find the "Delete" button for the wrestler and click it
    WebElement deleteButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[@id='delete-" + wrestler.getId() + "']")));

    clickAndScrollIntoView(deleteButton);

    // Verify that the wrestler is removed from the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Wrestler to Delete"));
          } catch (Exception e) {
            return false;
          }
        });
    assertEquals(initialSize - 1, wrestlerRepository.count());
  }
}
